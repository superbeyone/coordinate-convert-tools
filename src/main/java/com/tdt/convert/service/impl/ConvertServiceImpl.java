package com.tdt.convert.service.impl;

import com.tdt.convert.config.TdtConfig;
import com.tdt.convert.service.ConvertService;
import com.tdt.convert.service.GeoJsonReaderService;
import com.tdt.convert.service.ShapeBigReaderService;
import com.tdt.convert.thread.TdtExecutor;
import com.tdt.convert.utils.CoordinateUtil;
import com.tdt.convert.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className ConvertServiceImpl
 * @description
 * @date 2021-02-20 17:37
 **/

@Slf4j
@Service
public class ConvertServiceImpl implements ConvertService {


    @Autowired
    TdtConfig tdtConfig;

    @Autowired
    CoordinateUtil coordinateUtil;
    @Autowired
    TdtExecutor tdtExecutor;
    @Autowired
    GeoJsonReaderService geoJsonReaderService;
    @Autowired
    ShapeBigReaderService shapeReaderService;

    @Override
    public void convertFile() {
        File inputFile = new File(tdtConfig.getInput());
        if (!inputFile.exists()) {
            log.error("源数据文件夹[ {} ]不存在，请确认后，再进行操作......", tdtConfig.getInput());
            return;
        }
        List<File> fileList = getFileFromZip(inputFile, new ArrayList<>());

        log.info("系统初始化完成.....");

        List<File> geoJsonList = new ArrayList<>(fileList.size());
        List<File> shpList = new ArrayList<>(fileList.size());
        for (File file : fileList) {
            String name = file.getName();
            if (StringUtils.endsWithIgnoreCase(name, ".geojson")
                    || StringUtils.endsWithIgnoreCase(name, ".json")) {
                geoJsonList.add(file);
            } else if (StringUtils.endsWithIgnoreCase(name, ".shp")) {
                shpList.add(file);
            }
        }

        File outPutDirectory = new File(tdtConfig.getOutput());

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        File outPutRoot = new File(outPutDirectory, now);
        if (!outPutRoot.exists()) {
            log.info("输出数据文件夹[ {} ]不存在，系统自动创建......\n", outPutRoot.getAbsolutePath());
            outPutRoot.mkdirs();
        }

        if (geoJsonList.size() > 0) {
            //geoJson
            geoJsonReaderService.readGeoJson(geoJsonList, outPutRoot);
        }
        if (shpList.size() > 0) {
            //shape
            shapeReaderService.readShape(shpList, outPutRoot);
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
     * 从zip中获取文件
     *
     * @param inputFile 输入文件夹
     * @param files     集合
     * @return 结果集
     */
    private List<File> getFileFromZip(File inputFile, List<File> files) {
        if (inputFile.isDirectory()) {
            File[] listFiles = inputFile.listFiles();
            for (File file : listFiles) {
                getFileFromZip(file, files);
            }
        } else {
            String name = inputFile.getName();
            if (StringUtils.endsWithIgnoreCase(name, ".zip")) {
                File destDir = new File(inputFile.getParentFile(), name);
                //解压
                FileUtil.unpack2(inputFile, destDir, tdtConfig.getCharset());
                getFileFromZip(destDir, files);
            } else if (StringUtils.endsWithIgnoreCase(name, ".geojson")
                    || StringUtils.endsWithIgnoreCase(name, ".json")) {
                files.add(inputFile);
                log.info("找到文件[ {} ]，并添加到任务队列，当前共 [ {} ]个任务需要处理...", inputFile.getAbsolutePath(), files.size());
            } else if (StringUtils.endsWithIgnoreCase(name, ".shp")) {
                files.add(inputFile);
                log.info("找到文件[ {} ]，并添加到任务队列，当前共 [ {} ]个任务需要处理...", inputFile.getAbsolutePath(), files.size());
            }
        }
        return files;
    }

}
