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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
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

    private int type;

    private AtomicInteger taskNum = new AtomicInteger(0);
    private AtomicInteger taskCount = new AtomicInteger(0);


    @Override
    public void readGeoJson() {
        type = tdtConfig.getType();
        File inputFile = new File(tdtConfig.getInput());
        if (!inputFile.exists()) {
            log.error("源数据文件夹[ {} ]不存在，请确认后，再进行操作......", tdtConfig.getInput());
            return;
        }
        File outPutDirectory = new File(tdtConfig.getOutput());

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        File outPutRoot = new File(outPutDirectory, now);
        if (!outPutRoot.exists()) {
            log.info("输出数据文件夹[ {} ]不存在，系统自动创建......\n", outPutRoot.getAbsolutePath());
            outPutRoot.mkdirs();
        }
        List<File> fileList = getZipAndGeoJsonFile(inputFile, new ArrayList<>());
        taskCount.set(fileList.size());
        ExecutorService executorService = tdtExecutor.getExecutorService();
        for (File file : fileList) {

            String fileName = file.getName();
            int num = taskNum.incrementAndGet();
            try {
                System.out.println();
                System.out.println();
                log.info("=====================================================================");
                log.info("||\t\t\t开始处理第 [ {} ]个任务，共 [ {} ]个任务\t\t\t||", num, fileList.size());
                log.info("=====================================================================\n\n");
                if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
                    //从zip中处理数据
                    readFromZip(file, outPutRoot, executorService);
                } else {
                    executorService.execute(() -> {
                        String entryName = StringUtils.substringAfter(file.getAbsolutePath(), inputFile.getAbsolutePath());
                        File outFile = new File(outPutRoot, entryName);
                        File parentFile = outFile.getParentFile();
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        //处理GeoJson文件
                        readFromGeoJson(file, outFile);

                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        checkOutPutDir(outPutRoot);
        log.info("=========================[ 程序执行结束 ]=========================");
        System.out.println();
        log.info("=========================[ 程序执行结束 ]=========================");
        System.out.println();
        log.info("=========================[ 程序执行结束 ]=========================");
    }

    private void checkOutPutDir(File outPutDir) {
        if (outPutDir.exists() && outPutDir.isDirectory() && outPutDir.listFiles().length == 0) {
            outPutDir.delete();
        }
    }

    /**
     * 查找所有的zip和 geoJson 文件
     *
     * @param inputFile 输入文件夹
     * @param files     集合
     * @return 结果集
     */
    private List<File> getZipAndGeoJsonFile(File inputFile, List<File> files) {
        if (inputFile.isDirectory()) {
            File[] listFiles = inputFile.listFiles();
            for (File file : listFiles) {
                getZipAndGeoJsonFile(file, files);
            }
        } else {
            String name = inputFile.getName();
            if (StringUtils.endsWithIgnoreCase(name, ".zip")
                    || StringUtils.endsWithIgnoreCase(name, ".geojson")
                    || StringUtils.endsWithIgnoreCase(name, ".json")) {
                files.add(inputFile);
                log.info("找到文件[ {} ]，并添加到任务队列，当前共 [ {} ]个任务需要处理...", inputFile.getAbsolutePath(), files.size());
            }
        }
        return files;
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
    private void readFromZip(File file, File outPutRoot, ExecutorService executorService) {
        BufferedReader reader = null;
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
                        readGeoJsonData(zin, entry.getName(), reader, outPutRoot, executorService);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private void readGeoJsonData(InputStream inputStream, String entryName, BufferedReader reader, File outPutRoot, ExecutorService executorService) {
        try {
            reader = new BufferedReader(new UnicodeReader(inputStream, tdtConfig.getCharset()));
            String line;
            StringBuilder lineBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                lineBuilder.append(line);
            }
            executorService.execute(() -> {
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
            });
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
            for (GeoFeatures tdtFeature : tdtFeatures) {
                num++;
                log.info("\t\t\t--- 开始处理第[ {} ]条数据,共[ {} ]条数据,来自文件 [ {} ] \t当前执行第[ {} ]个任务,共[ {} ]个任务", num, tdtFeatures.size(), entryName, taskNum, taskCount);
                //远程请求百度接口
//                tdtFeature.setProperties(convertTdtGeoProperties2BaiDuRemote(tdtFeature.getProperties()));
                //本地算法转换
                tdtFeature.setProperties(tdtFeature.getProperties());

                tdtFeature.setGeometry(convertTdtGeoGeometry2BaiDu(tdtFeature.getGeometry()));

                baiDuGeoFeatures.add(tdtFeature);
                log.info("\t\t\t--- 第[ {} ]条数据处理完成,共[ {} ]条数据,来自文件 [ {} ] \t当前执行第[ {} ]个任务,共[ {} ]个任务", num, tdtFeatures.size(), entryName, taskNum, taskCount);
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

}
