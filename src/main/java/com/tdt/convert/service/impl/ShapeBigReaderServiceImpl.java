package com.tdt.convert.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tdt.convert.config.TdtConfig;
import com.tdt.convert.service.GeoJsonReaderService;
import com.tdt.convert.service.ShapeBigReaderService;
import com.tdt.convert.thread.TdtExecutor;
import com.vividsolutions.jts.geom.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className ShapeBigReaderServiceImpl
 * @description
 * @date 2021-02-22 18:50
 **/

@Slf4j
@Service
public class ShapeBigReaderServiceImpl implements ShapeBigReaderService {


    @Autowired
    TdtConfig tdtConfig;

    @Autowired
    GeoJsonReaderService geoJsonReaderService;

    @Autowired
    TdtExecutor tdtExecutor;

    private AtomicInteger taskNum = new AtomicInteger(0);
    private AtomicInteger taskCount = new AtomicInteger(0);

    private static final int SINGLE_FILE_COUNT = 1000;

    private static final int DECIMALS = 20;

    /**
     * 读取shp文件
     *
     * @param shpList    shp
     * @param outPutRoot 输出文件路径
     */
    @Override
    public void readShape(List<File> shpList, File outPutRoot) {
        String charset = tdtConfig.getCharset();
        String input = tdtConfig.getInput();
        taskCount.set(shpList.size());
        for (File file : shpList) {
            int num = taskNum.incrementAndGet();
            System.out.println();
            System.out.println();
            log.info("===================== Shape ===============================");
            log.info("||\t\t\t开始处理第 [ {} ]个任务，共 [ {} ]个任务\t\t\t||", num, shpList.size());
            log.info("===================== Shape ===============================\n\n");
            String absolutePath = file.getAbsolutePath();
            if (StringUtils.contains(input, "/")) {
                input = StringUtils.replace(input, "/", "\\");
            }
            String filePath = StringUtils.substringAfter(absolutePath, input);
            File outputShapeFile = new File(outPutRoot, filePath);
            List<String> geoJsonList = convertShp2GeoJson(file, charset);

            List<String> resultJsonList = new ArrayList<>(geoJsonList.size());
            for (String geoJson : geoJsonList) {

                String resultJson = geoJsonReaderService.convertGeoJson(geoJson, absolutePath, num, shpList.size());
                resultJsonList.add(resultJson);
            }

            //输出shape
            geoJson2Shape(resultJsonList, outputShapeFile);
        }
    }

    private List<String> convertShp2GeoJson(File file, String charset) {
        log.info("Shape文件[ {} ] 转 GeoJson 开始", file.getAbsolutePath());

        List<String> geoJsonList = new ArrayList<>();
        StringWriter writer = new StringWriter();
        try {
            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(file.toURI().toURL());
            shapefileDataStore.setCharset(Charset.forName(charset));

            ContentFeatureSource featureSource = shapefileDataStore.getFeatureSource();

            ContentFeatureCollection features = featureSource.getFeatures();
            FeatureJSON featureJSON = new FeatureJSON(new GeometryJSON(DECIMALS));
            SimpleFeatureIterator iterator = features.features();
            List<SimpleFeature> simpleFeatures = new LinkedList<>();
            SimpleFeatureType featureType = null;
            while (iterator.hasNext()) {
                if (simpleFeatures.size() == SINGLE_FILE_COUNT) {
                    SimpleFeatureCollection collection = new ListFeatureCollection(featureType, simpleFeatures);
                    featureJSON.writeFeatureCollection(collection, writer);
                    String geoJson = writer.toString();
                    geoJsonList.add(geoJson);
                    simpleFeatures = new LinkedList<>();
                    writer = new StringWriter();
                }
                SimpleFeature feature = iterator.next();
                featureType = feature.getFeatureType();
                simpleFeatures.add(feature);
            }
            SimpleFeatureCollection collection = new ListFeatureCollection(featureType, simpleFeatures);
            featureJSON.writeFeatureCollection(collection, writer);
            String geoJson = writer.toString();
            //最后一批次
            if (StringUtils.isNotBlank(geoJson)) {
                geoJsonList.add(geoJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("Shape文件[ {} ] 转 GeoJson 完成", file.getAbsolutePath());
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return geoJsonList;
    }

    private void geoJson2Shape(List<String> geojsonStrList, File file) {

        try {
            String fileName = file.getName();
            if (!StringUtils.endsWithIgnoreCase(fileName, ".shp")) {
                file = new File(file.getParentFile(), fileName + ".shp");
            }
            GeometryJSON geometryJSON = new GeometryJSON(DECIMALS);
            //创建shape文件对象
            Map<String, Serializable> params = new HashMap<>();
            params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
            ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

            Map<String, Class> mapFields = new HashMap(64);
            //定义图形信息和属性信息
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setCRS(DefaultGeographicCRS.WGS84);
            tb.setName("shapefile");

            String geojsonType = null;
            Class<?> geoType = null;

            List<Map> featuresList = new ArrayList<>(geojsonStrList.size());

            log.info("正则查找最大属性集");
            for (String geoJson : geojsonStrList) {

                Map<String, Object> geojsonMap = JSONObject.parseObject(geoJson, Map.class);
                List<Map> features = (List<Map>) geojsonMap.get("features");
                featuresList.addAll(features);

                Map geojsonExample = features.get(0);
                if (geoType == null) {
                    geojsonType = ((Map) geojsonExample.get("geometry")).get("type").toString();
                }
                for (int i = 0; i < features.size(); i++) {
                    Map oneGeoJsonMap = features.get(i);
                    Map<String, Object> attributes = (Map<String, Object>) oneGeoJsonMap.get("properties");
                    for (String key : attributes.keySet()) {
                        Class type = attributes.get(key).getClass();
                        mapFields.put(key, type);
                    }
                }
            }
            for (String key : mapFields.keySet()) {
                tb.add(key, mapFields.get(key));
            }
            switch (geojsonType) {
                case "Point":
                    geoType = Point.class;
                    break;
                case "MultiPoint":
                    geoType = MultiPoint.class;
                    break;
                case "LineString":
                    geoType = LineString.class;
                    break;
                case "MultiLineString":
                    geoType = MultiLineString.class;
                    break;
                case "Polygon":
                    geoType = Polygon.class;
                    break;
                case "MultiPolygon":
                    geoType = MultiPolygon.class;
                    break;
                default:
                    geoType = Geometry.class;
            }
            tb.add("the_geom", geoType);

            ds.createSchema(tb.buildFeatureType());
            log.info("查找最大属性集完成");
            //设置Writer
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.
                    getFeatureWriter(ds.getTypeNames()[0], Transaction.AUTO_COMMIT);

            log.info("开始输出写入 shp 文件，[{}]", file.getAbsolutePath());
            int index = 1;
            for (Map oneGeojson : featuresList) {
                log.info("开始输出[ {} / {} ]要素，写入shp文件，[ {} ]", index++, featuresList.size(), file.getAbsolutePath());
                Map<String, Object> attributes = (Map<String, Object>) oneGeojson.get("properties");
                String strFeature = JSONObject.toJSONString(oneGeojson);
                Reader reader = new StringReader(strFeature);
                SimpleFeature feature = writer.next();

                switch (geojsonType) {
                    case "Point":
                        feature.setAttribute("the_geom", geometryJSON.readPoint(reader));
                        break;
                    case "MultiPoint":
                        feature.setAttribute("the_geom", geometryJSON.readMultiPoint(reader));
                        break;
                    case "LineString":
                        feature.setAttribute("the_geom", geometryJSON.readLine(reader));
                        break;
                    case "MultiLineString":
                        feature.setAttribute("the_geom", geometryJSON.readMultiLine(reader));
                        break;
                    case "Polygon":
                        feature.setAttribute("the_geom", geometryJSON.readPolygon(reader));
                        break;
                    case "MultiPolygon":
                        feature.setAttribute("the_geom", geometryJSON.readMultiPolygon(reader));
                        break;
                    default:
                }

                for (String key : attributes.keySet()) {
                    feature.setAttribute(key, attributes.get(key));
                }
                writer.write();
            }
            log.info("输出写入 shp 文件完成，[ {} ]", file.getAbsolutePath());
            writer.close();
            ds.dispose();
            //生成编码文件
            generateCpgFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateCpgFile(File root) {
        String filename = root.getName();
        String name = StringUtils.substringBefore(filename, ".");
        File file = new File(StringUtils.endsWithIgnoreCase(filename, ".shp") ? root.getParentFile() : root, name + ".cpg");
        log.info("开始写入编码文件，[ {} ]", file.getAbsolutePath());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("GBK,GB2312,UTF-8");
            writer.flush();
            log.info("写入编码文件完成，[ {} ]", file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
