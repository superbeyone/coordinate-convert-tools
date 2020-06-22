package com.tdt.convert.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tdt.convert.commons.TdtConst;
import com.tdt.convert.config.TdtConfig;
import com.tdt.convert.entity.GeoFeatures;
import com.tdt.convert.entity.GeoGeometry;
import com.tdt.convert.entity.GeoProperties;
import com.tdt.convert.entity.GeoRegion;
import com.tdt.convert.utils.HttpClientUtil;
import com.tdt.convert.utils.UnicodeReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className GeoJsonTest
 * @description
 * @date 2020-04-09 13:08
 **/

@Slf4j
public class GeoJsonTest {

    @Autowired
    TdtConfig tdtConfig;

    @Test
    public void testStringTxt() {
        String txt = "[[[[113.575775,22.16965],[113.575775,22.16965]]]]";
        JSONObject jsonObject = JSONObject.parseObject(txt);
        if (jsonObject instanceof List) {
            List list = (List) jsonObject;
        }
    }

    private Map<Integer, Object> getCoordinates(Object tdtCoordinates, int count, Map<Integer, Object> map) {
        if (tdtCoordinates instanceof List) {
            List coordinates = (List) tdtCoordinates;
            for (Object coordinate : coordinates) {
                getCoordinates(coordinate, count, map);
            }
        } else {
            map.put(count++, tdtCoordinates);
        }
        return map;
    }

    /**
     * 递归获取 Coordinates 没有实现
     *
     * @param tdtCoordinates tdt
     * @param builder        builder
     * @return baidu
     */
    @Deprecated
    private StringBuilder generateCoordinates(Object tdtCoordinates, StringBuilder builder, int count) {

        if (tdtCoordinates instanceof List) {
            List coordinates = (List) tdtCoordinates;
            if ((coordinates).size() > 2) {
                //集合长度大于2，一定是数组
                for (Object coordinate : coordinates) {
                    generateCoordinates(coordinate, builder, count);
                }
            } else {
                int size = 0;
                StringBuilder lngLatBuilder = new StringBuilder();
                //集合长度小于等于2
                for (Object coordinate : coordinates) {
                    if (coordinate instanceof List) {
                        generateCoordinates(coordinate, builder, count);
                    } else {
                        //是数据
                        lngLatBuilder.append(coordinate);
                        size++;
                        count++;
                        if (size == 1) {
                            lngLatBuilder.append(",");
                        }
                    }
                }
                if (size == 2) {
                    Object baiDuLngLat = getBaiDuLngLat(lngLatBuilder.toString());
                    if (baiDuLngLat instanceof List) {
                        String baiDuLngLatStr = ((List) baiDuLngLat).get(0).toString();
                        JSONObject jsonObject = JSONObject.parseObject(baiDuLngLatStr);
                        Double x = (Double) jsonObject.get("x");
                        Double y = (Double) jsonObject.get("y");
                        builder.append("[").append(x).append(",").append(y).append("]");
                    }
                }

            }
        }

        return builder;
    }


    @Test
    public void testConvertList() {
        List<Object> coordinates = new LinkedList<>();
        List<Object> coordinates1 = new LinkedList<>();
        List<Object> coordinates2 = new LinkedList<>();
        List<Object> coordinates3 = new LinkedList<>();
        coordinates3.add(113.575775);
        coordinates3.add(22.16965);

        List<Object> coordinates4 = new LinkedList<>();
        coordinates4.add(113.575775);
        coordinates4.add(22.16965);

        coordinates2.add(coordinates3);
        coordinates2.add(coordinates4);
        coordinates1.add(coordinates2);
        coordinates.add(coordinates1);

        System.out.println(coordinates.toString());

    }

    @Test
    public void testGeoJsonZip() {
        File inputFile = new File(tdtConfig.getInput());
        if (!inputFile.exists()) {
            log.error("源数据文件夹[ {} ]不存在，请确认后，再进行操作......", tdtConfig.getInput());
            return;
        }
        File outPutDirectory = new File(tdtConfig.getOutput());
        if (outPutDirectory.exists()) {
            log.info("输出数据文件夹不存在，系统自动创建......当前时间:\t" + LocalDateTime.now().toString());
        }
        File outPutRoot = new File(outPutDirectory, LocalDateTime.now().toString());
        if (!outPutRoot.exists()) {
            outPutRoot.mkdirs();
        }
        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFile));
            readFromZip(inputStream, outPutRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void readFromZip(InputStream inputStream, File outPutRoot) {
        BufferedReader reader = null;
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new BufferedInputStream(inputStream)), Charset.forName(tdtConfig.getCharset()));
        ) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (StringUtils.endsWithIgnoreCase(entry.getName(), ".geoJson")) {
                        readGeoJsonData(zin, entry, reader, outPutRoot);
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

    public void readGeoJsonData(InputStream inputStream, ZipEntry entry, BufferedReader reader, File outPutRoot) {
        try {
            reader = new BufferedReader(new UnicodeReader(inputStream, tdtConfig.getCharset()));
            String line;
            StringBuilder lineBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                lineBuilder.append(line);
            }

            GeoRegion geoRegion = JSONObject.parseObject(lineBuilder.toString(), GeoRegion.class);
            System.out.println(geoRegion.toString());
            File outFile = new File(outPutRoot, entry.getName());
            File parentFile = outFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            //经纬度转换
            geoRegion = convertTdtGeoRegion2BaiDu(geoRegion);

            //输出文件
            exportGeoJsonFile(outFile, geoRegion);
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
    private GeoRegion convertTdtGeoRegion2BaiDu(GeoRegion tdtGeoRegion) {
//        GeoRegion baiDuGeoRegion = new GeoRegion();
//        baiDuGeoRegion.setType(tdtGeoRegion.getType());
        tdtGeoRegion.setFeatures(convertTdtGeoFeatures2BaiDu(tdtGeoRegion.getFeatures()));

        return tdtGeoRegion;
    }

    private List<GeoFeatures> convertTdtGeoFeatures2BaiDu(List<GeoFeatures> tdtFeatures) {
        List<GeoFeatures> baiDuGeoFeatures = new LinkedList<>();
        if (tdtFeatures != null && tdtFeatures.size() > 0) {
            for (GeoFeatures tdtFeature : tdtFeatures) {
                tdtFeature.setProperties(convertTdtGeoProperties2BaiDu(tdtFeature.getProperties()));
                tdtFeature.setGeometry(convertTdtGeoGeometry2BaiDu(tdtFeature.getGeometry()));

                baiDuGeoFeatures.add(tdtFeature);
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
     * @param tdtCoordinates 百度
     * @return 百度
     */
    private Object convertTdtCoordinates2BaiDu(Object tdtCoordinates) {
        //
//        return CoordinatesUtil.getBaiDuCoordinates(tdtCoordinates);
        return tdtCoordinates;
    }


    /**
     * 转换 tdtProperties 为百度
     *
     * @param tdtProperties 天地图
     * @return 百度
     */
    private GeoProperties convertTdtGeoProperties2BaiDu(GeoProperties tdtProperties) {
        GeoProperties baiDuProperties = new GeoProperties();

        //基础信息复制
        BeanUtils.copyProperties(tdtProperties, baiDuProperties, TdtConst.IGNORE_PROPERTIES);

        //构造基本点坐标
        StringBuilder builder = new StringBuilder();
        String lngLatStr = builder.append(tdtProperties.getX()).append(",").append(tdtProperties.getY())
                .append(";")
                .append(tdtProperties.getXMAX()).append(",").append(tdtProperties.getYMAX())
                .append(";")
                .append(tdtProperties.getXMIN()).append(",").append(tdtProperties.getYMIN()).toString();
        //获取百度的坐标
        Object baiDuLngLatObj = getBaiDuLngLat(lngLatStr);
        if (baiDuLngLatObj instanceof List) {
            List baiDuLngLatList = (List) baiDuLngLatObj;
            for (int i = 0; i < (baiDuLngLatList).size(); i++) {
                JSONObject lngLat = JSON.parseObject(baiDuLngLatList.get(i).toString());
                //经度
                BigDecimal x = (BigDecimal) lngLat.get("x");
                //纬度
                BigDecimal y = (BigDecimal) lngLat.get("y");
                switch (i) {
                    case 0:
//                        baiDuProperties.setX(x);
//                        baiDuProperties.setY(y);
                        break;
                    case 1:
//                        baiDuProperties.setXMAX(x);
//                        baiDuProperties.setYMAX(y);
                        break;
                    case 3:
//                        baiDuProperties.setXMIN(x);
//                        baiDuProperties.setYMIN(y);
                        break;
                    default:
                }
            }
        }
        return baiDuProperties;
    }


    private Object getBaiDuLngLat(String lngLatStr) {
        StringBuilder urlBuilder = new StringBuilder(TdtConst.BAI_DU_CONVERT_URL_PREFIX)
                .append(lngLatStr).append(TdtConst.BAI_DU_CONVERT_URL_SUFFIX);

        String jsonResult = HttpClientUtil.sendHttpGet(urlBuilder.toString());

        JSONObject jsonObject = JSON.parseObject(jsonResult);
        Object result = jsonObject.get("result");
        if (result != null) {
            return result;

        } else {
            log.error("请求百度接口异常，经纬度转换失败，将使用原始数据作为替换方案，请求参数,[ {} ]", lngLatStr);
            System.out.println(jsonResult);
        }
        return null;

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
