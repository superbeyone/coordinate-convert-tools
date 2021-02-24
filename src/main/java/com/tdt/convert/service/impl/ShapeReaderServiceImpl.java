package com.tdt.convert.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tdt.convert.config.TdtConfig;
import com.tdt.convert.entity.GeoRegion;
import com.tdt.convert.service.GeoJsonReaderService;
import com.tdt.convert.service.ShapeReaderService;
import com.tdt.convert.thread.TdtExecutor;
import com.vividsolutions.jts.geom.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className ShapeReaderServiceImpl
 * @description
 * @date 2021-02-20 17:53
 **/
@Slf4j
@Service
public class ShapeReaderServiceImpl implements ShapeReaderService {


    @Autowired
    TdtConfig tdtConfig;

    @Autowired
    GeoJsonReaderService geoJsonReaderService;

    @Autowired
    TdtExecutor tdtExecutor;

    private AtomicInteger taskNum = new AtomicInteger(0);
    private AtomicInteger taskCount = new AtomicInteger(0);

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
            String geoJson = convertShp2GeoJson(file, charset);
            GeoRegion geoRegion = JSONObject.parseObject(geoJson, GeoRegion.class);
            String resultJson = geoJsonReaderService.convertGeoJson(geoRegion, absolutePath, num, shpList.size());
            //输出shape
            geoJson2Shape(resultJson, outputShapeFile);
        }
    }


    private String convertShp2GeoJson(File file, String charset) {
        log.info("Shape文件[ {} ] 转 GeoJson 开始", file.getAbsolutePath());

        StringWriter writer = new StringWriter();
        try {
            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(file.toURI().toURL());
            shapefileDataStore.setCharset(Charset.forName(charset));

            ContentFeatureSource featureSource = shapefileDataStore.getFeatureSource();

            ContentFeatureCollection features = featureSource.getFeatures();
            FeatureJSON featureJSON = new FeatureJSON(new GeometryJSON(10));

            featureJSON.writeFeatureCollection(features, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Shape文件[ {} ] 转 GeoJson 完成", file.getAbsolutePath());
        return writer.toString();
    }


    private static void geoJson2Shape(String geojsonStr, File file) {
        String fileName = file.getName();
        if (!StringUtils.endsWithIgnoreCase(fileName, ".shp")) {
            file = new File(file.getParentFile(), fileName + ".shp");
        }
        GeometryJSON geojson = new GeometryJSON(20);
        try {
            Map<String, Object> geojsonMap = JSONObject.parseObject(geojsonStr, Map.class);
            List<Map> features = (List<Map>) geojsonMap.get("features");
            Map geojsonExample = features.get(0);
            String geojsonType = ((Map) geojsonExample.get("geometry")).get("type").toString();
            Map<String, Class> mapFields = new HashMap();
            for (int i = 0; i < features.size(); i++) {
                Map<String, Object> attributes = (Map<String, Object>) features.get(i).get("properties");
                for (String key : attributes.keySet()) {
                    Class type = attributes.get(key).getClass();
                    mapFields.put(key, type);
                }
            }

            Class<?> geoType = null;
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
            }

            //创建shape文件对象
            Map<String, Serializable> params = new HashMap<>();
            params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
            ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);
            //定义图形信息和属性信息
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setCRS(DefaultGeographicCRS.WGS84);
            tb.setName("shapefile");
            tb.add("the_geom", geoType);

            for (String key : mapFields.keySet()) {
                tb.add(key, mapFields.get(key));
            }

            ds.createSchema(tb.buildFeatureType());
            //设置Writer
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.
                    getFeatureWriter(ds.getTypeNames()[0], Transaction.AUTO_COMMIT);

            for (int i = 0, len = features.size(); i < len; i++) {
                Map oneGeojson = features.get(i);
                Map<String, Object> attributes = (Map<String, Object>) oneGeojson.get("properties");
                String strFeature = JSONObject.toJSONString(oneGeojson);
                Reader reader = new StringReader(strFeature);
                SimpleFeature feature = writer.next();

                switch (geojsonType) {
                    case "Point":
                        feature.setAttribute("the_geom", geojson.readPoint(reader));
                        break;
                    case "MultiPoint":
                        feature.setAttribute("the_geom", geojson.readMultiPoint(reader));
                        break;
                    case "LineString":
                        feature.setAttribute("the_geom", geojson.readLine(reader));
                        break;
                    case "MultiLineString":
                        feature.setAttribute("the_geom", geojson.readMultiLine(reader));
                        break;
                    case "Polygon":
                        feature.setAttribute("the_geom", geojson.readPolygon(reader));
                        break;
                    case "MultiPolygon":
                        feature.setAttribute("the_geom", geojson.readMultiPolygon(reader));
                        break;
                }

                for (String key : attributes.keySet()) {
                    feature.setAttribute(key, attributes.get(key));
                }
                writer.write();
            }
            writer.close();
            ds.dispose();
        } catch (Exception e) {
            System.out.println("转换失败");
            e.printStackTrace();
        }
        //生成编码文件
        generateCpgFile(file);
    }

    private static void generateCpgFile(File root) {
        String filename = root.getName();
        String name = StringUtils.substringBefore(filename, ".");
        File file = new File(StringUtils.endsWithIgnoreCase(filename, ".shp") ? root.getParentFile() : root, name + ".cpg");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("GBK,GB2312,UTF-8");
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
