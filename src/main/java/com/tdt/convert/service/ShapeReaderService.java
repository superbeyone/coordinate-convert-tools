package com.tdt.convert.service;

import java.io.File;
import java.util.List;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className ShapeReaderService
 * @description shp读取
 * @date 2021-02-20 17:53
 **/

public interface ShapeReaderService {

    /**
     * 读取shp文件
     *
     * @param shpList shp
     * @param outPutRoot 输出文件路径
     */
    void readShape(List<File> shpList,File outPutRoot);
}
