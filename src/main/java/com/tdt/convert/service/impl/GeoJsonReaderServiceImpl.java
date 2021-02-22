package com.tdt.convert.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tdt.convert.config.TdtConfig;
import com.tdt.convert.entity.GeoFeatures;
import com.tdt.convert.entity.GeoGeometry;
import com.tdt.convert.entity.GeoRegion;
import com.tdt.convert.service.GeoJsonReaderService;
import com.tdt.convert.thread.TdtExecutor;
import com.tdt.convert.utils.CoordinateUtil;
import com.tdt.convert.utils.UnicodeReader;
import com.tdt.convert.utils.coding.EncodingDetect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className GeoJsonReaderServiceImpl
 * @description
 * @date 2020-04-09 12:32
 **/

@Slf4j
@Service
public class GeoJsonReaderServiceImpl implements GeoJsonReaderService {

    @Autowired
    TdtConfig tdtConfig;

    @Autowired
    CoordinateUtil coordinateUtil;
    @Autowired
    TdtExecutor tdtExecutor;


    private AtomicInteger taskNum = new AtomicInteger(0);
    private AtomicInteger taskCount = new AtomicInteger(0);


    @Override
    public void readGeoJson(List<File> fileList, File outPutRoot) {
        File inputFile = new File(tdtConfig.getInput());
        if (!inputFile.exists()) {
            log.error("源数据文件夹[ {} ]不存在，请确认后，再进行操作......", tdtConfig.getInput());
            return;
        }

        taskCount.set(fileList.size());
        for (File file : fileList) {

            String fileName = file.getName();
            int num = taskNum.incrementAndGet();
            try {
                System.out.println();
                System.out.println();
                log.info("========================== GeoJson ======================================");
                log.info("||\t\t\t开始处理第 [ {} ]个任务，共 [ {} ]个任务\t\t\t||", num, fileList.size());
                log.info("========================== GeoJson ==================================\n\n");
                if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
                    //从zip中处理数据
                    readFromZip(file, outPutRoot);
                } else {
                        String entryName = StringUtils.substringAfter(file.getAbsolutePath(), inputFile.getAbsolutePath());
                        File outFile = new File(outPutRoot, entryName);
                        File parentFile = outFile.getParentFile();
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        //处理GeoJson文件
                        readFromGeoJson(file, outFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }


    /**
     * 读取 GeoJson 文件
     *
     * @param geoJsonFile geoJson文件
     * @param outFile     输出文件
     */
    private void readFromGeoJson(File geoJsonFile, File outFile) {
        String encode = EncodingDetect.getFileEncode(geoJsonFile);
        log.info("系统检测到文件[ {} ]使用编码格式为：[ {} ]", geoJsonFile.getAbsolutePath(), encode);

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(geoJsonFile));
             BufferedReader reader = new BufferedReader(new UnicodeReader(inputStream, encode))) {
            String line;
            StringBuilder lineBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                lineBuilder.append(line);
            }
            GeoRegion geoRegion = JSONObject.parseObject(lineBuilder.toString(), GeoRegion.class);


            log.info("开始转换文件 [ {} ]", geoJsonFile.getAbsolutePath());
            //经纬度转换
            geoRegion = convertTdtGeoRegion2BaiDu(geoRegion, geoJsonFile.getAbsolutePath());
            log.info("结束转换文件 [ {} ]，开始输出结果文件", geoJsonFile.getAbsolutePath());
            //输出文件
            exportGeoJsonFile(outFile, geoRegion);
            log.info("输出成功，文件路径 [ {} ]\n\n", outFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从zip中读取数据
     *
     * @param file       zip文件
     * @param outPutRoot 输出路径
     */
    private void readFromZip(File file, File outPutRoot) {
        int count = getGeoJsonFileCount(file);
        if (count == 0) {
            log.info("ZIP 文件[ {} ]下没有找到GeoJson文件，该ZIP不做处理", file.getAbsolutePath());
            return;
        } else {
            System.out.println();
            log.info("ZIP 文件[ {} ]下找到[ {} ]个GeoJson文件\n", file.getAbsolutePath(), count);
        }
        AtomicInteger index = new AtomicInteger(1);
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new BufferedInputStream(new FileInputStream(file))), Charset.forName(tdtConfig.getCharset()));
        ) {
            log.info("开始处理 ZIP 文件 [ {} ]，文件内容列表如下：\t\t当前执行第[ {} ]个任务，共[ {} ]个任务", file, taskNum, taskCount);
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (StringUtils.endsWithIgnoreCase(entry.getName(), ".geoJson")) {
                        log.info("\t--- 开始处理该ZIP下第[ {} ]个GeoJson文件，共[ {} ]个GeoJson文件\t\t当前执行第[ {} ]个任务，共[ {} ]个任务", index.getAndIncrement(), count, taskNum, taskCount);
                        readGeoJsonData(zin, entry.getName(), outPutRoot);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取zip文件下符合格式的文件数量
     *
     * @param file zip文件
     * @return 数量
     */
    private int getGeoJsonFileCount(File file) {
        int count = 0;
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new BufferedInputStream(new FileInputStream(file))), Charset.forName(tdtConfig.getCharset()));
        ) {
            log.info("开始计算 ZIP 文件 [ {} ]下，GeoJson文件数量...", file);
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (StringUtils.endsWithIgnoreCase(entry.getName(), ".geoJson")) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private void readGeoJsonData(InputStream inputStream, String entryName, File outPutRoot) {
        try (BufferedReader reader = new BufferedReader(new UnicodeReader(inputStream, tdtConfig.getCharset()));) {

            String line;
            StringBuilder lineBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                lineBuilder.append(line);
            }
            GeoRegion geoRegion = JSONObject.parseObject(lineBuilder.toString(), GeoRegion.class);
            File outFile = new File(outPutRoot, entryName);
            File parentFile = outFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            log.info("\t\t--- 开始转换文件 [ {} ] \t当前执行第[ {} ]个任务,共[ {} ]个任务", entryName, taskNum, taskCount);
            //经纬度转换
            geoRegion = convertTdtGeoRegion2BaiDu(geoRegion, entryName);
            log.info("\t\t--- 结束转换文件 [ {} ],开始输出结果文件 \t当前执行第[ {} ]个任务,共[ {} ]个任务", entryName, taskNum, taskCount);
            //输出文件
            exportGeoJsonFile(outFile, geoRegion);
            log.info("\t\t--- 输出成功，文件路径 [ {} ] \t当前执行第[ {} ]个任务,共[ {} ]个任务\n\n", outFile.getAbsolutePath(), taskNum, taskCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 转换天地图经纬度为百度经纬度
     *
     * @param tdtGeoRegion 天地图经纬度
     * @return 百度
     */
    private GeoRegion convertTdtGeoRegion2BaiDu(GeoRegion tdtGeoRegion, String entryName) {
        tdtGeoRegion.setFeatures(convertTdtGeoFeatures2BaiDu(tdtGeoRegion.getFeatures(), entryName));
        return tdtGeoRegion;
    }


    private List<GeoFeatures> convertTdtGeoFeatures2BaiDu(List<GeoFeatures> tdtFeatures, String entryName) {
        List<GeoFeatures> baiDuGeoFeatures = new LinkedList<>();
        if (tdtFeatures != null && tdtFeatures.size() > 0) {
            int num = 0;
            ExecutorService executorService = tdtExecutor.getExecutorService();
            List<Future<GeoFeatures>> futures = new LinkedList<>();
            for (GeoFeatures tdtFeature : tdtFeatures) {
                num++;
                int finalNum = num;
                Future<GeoFeatures> future = executorService.submit(() -> {

                    log.info("\t\t\t--- 开始处理第[ {} ]条要素,共[ {} ]条要素,来自文件 [ {} ] \t当前执行第[ {} ]个任务,共[ {} ]个任务",
                            finalNum, tdtFeatures.size(), entryName, taskNum, taskCount);
                    //远程请求百度接口
//                tdtFeature.setProperties(convertTdtGeoProperties2BaiDuRemote(tdtFeature.getProperties()));
                    //本地算法转换
                    tdtFeature.setProperties(tdtFeature.getProperties());

                    tdtFeature.setGeometry(convertTdtGeoGeometry2BaiDu(tdtFeature.getGeometry()));


                    log.info("\t\t\t### 第[ {} ]条要素处理完成,共[ {} ]条要素,来自文件 [ {} ] \t当前执行第[ {} ]个任务,共[ {} ]个任务", finalNum, tdtFeatures.size(), entryName, taskNum, taskCount);
                    return tdtFeature;
                });
                futures.add(future);
            }

            log.info("tdtFeatures:[ {} ],\t futures:[{}]", tdtFeatures.size(), futures.size());
            for (Future<GeoFeatures> future : futures) {
                try {
                    GeoFeatures tdtFeature = future.get();
                    baiDuGeoFeatures.add(tdtFeature);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }


            try {
                executorService.shutdown();
                executorService.awaitTermination(2, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return baiDuGeoFeatures;
    }

    /**
     * 转换 tdtGeometry 为百度信息
     *
     * @param tdtGeometry 天地图坐标信息
     * @return 百度
     */
    private GeoGeometry convertTdtGeoGeometry2BaiDu(GeoGeometry tdtGeometry) {
        tdtGeometry.setCoordinates(convertTdtCoordinates2BaiDu(tdtGeometry.getCoordinates()));
        return tdtGeometry;
    }

    /**
     * 转换 tdtCoordinates 为百度
     *
     * @param tdtCoordinates 天地图
     * @return 百度
     */
    private Object convertTdtCoordinates2BaiDu(Object tdtCoordinates) {
        //TODO 转换 tdtCoordinates 为百度
        return coordinateUtil.getBaiDuCoordinates(tdtCoordinates);
    }

    /**
     * 输出Json文件
     *
     * @param outFile   输出文件
     * @param geoRegion geoJson实体对象
     */
    public void exportGeoJsonFile(File outFile, GeoRegion geoRegion) {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outFile))) {
            outputStream.write(JSON.toJSONString(geoRegion).getBytes());
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String convertGeoJson(String geoJson, String path, int num, int count) {
        log.info("Shape文件[ {} ] GeoJson 转 Java 对象", path);
        GeoRegion geoRegion = JSONObject.parseObject(geoJson, GeoRegion.class);
        this.taskNum.set(num);
        this.taskCount.set(count);
        log.info("开始坐标转换，文件 [ {} ]", path);
        //经纬度转换
        geoRegion = convertTdtGeoRegion2BaiDu(geoRegion, path);
        log.info("完成坐标转换，文件 [ {} ]", path);
        if (geoRegion == null) {
            System.out.println("-------------");
        }
        return JSON.toJSONString(geoRegion);
    }

}
