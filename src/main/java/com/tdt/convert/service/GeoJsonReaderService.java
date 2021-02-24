package com.tdt.convert.service;

import com.tdt.convert.entity.GeoRegion;

import java.io.File;
import java.util.List;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className GeoJsonReaderService
 * @description GeoJson数据读取
 * @date 2020-04-09 12:32
 **/

public interface GeoJsonReaderService {

    void readGeoJson(List<File> fileList,File outPutRoot);

    String convertGeoJson(GeoRegion geoRegion, String path, int num , int count);

}
