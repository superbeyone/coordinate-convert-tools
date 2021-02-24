package com.tdt.convert.service;

import com.alibaba.fastjson.JSONObject;
import com.tdt.convert.thread.TdtExecutor;
import com.vividsolutions.jts.geom.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className ShapExportTest
 * @description
 * @date 2021-02-24 09:49
 **/

@Slf4j
@SpringBootTest
public class ShapeExportTest {


    private int DECIMALS = 20;

    @Autowired
    TdtExecutor tdtExecutor;


    @Test
    public void testExecutor() {


        ExecutorService executorService = tdtExecutor.getExecutorService();
        method1("第一次");
        method1("第二次");
        method1("第三次");
        method1("第四次");
        method1("第五次");
        try {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void method1(String prefix) {
        ExecutorService executorService = tdtExecutor.getExecutorService();

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {

            int finalI = i;
            Future<Boolean> future = executorService.submit(() -> {
                log.info("prefix:[ {}-> {} ]", prefix, finalI);
                Thread.sleep(1000);
                return true;
            });
            futures.add(future);
        }
        try {
            for (Future<Boolean> future : futures) {
                future.get();
            }
          
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExport() {
        File convertedGeoJsonDir = new File("E:\\data\\shp\\test\\output\\20210223193853\\gis_osm_roads_free_1\\step_2_converted_geojson_dir");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        File shapeFile = new File("E:\\data\\shp\\test\\output\\20210223193853\\gis_osm_roads_free_1", now + "\\roads.shp");
        geoJson2Shape(convertedGeoJsonDir, shapeFile);

    }


    private void geoJson2Shape(File convertedGeoJsonDir, File file) {
        ExecutorService executorService = tdtExecutor.getExecutorService();
        try {
            String fileName = file.getName();
            if (!StringUtils.endsWithIgnoreCase(fileName, ".shp")) {
                file = new File(file.getParentFile(), fileName + ".shp");
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
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


            log.info("正在查找最大属性集");
            File[] geojsonStrFiles = convertedGeoJsonDir.listFiles();
//            for (int k = 0; k < geojsonStrFiles.length; k++) {
//                log.info("正在查找最大属性集,[ {} / {}]", k + 1, geojsonStrFiles.length);
            File geojsonStrFile0 = geojsonStrFiles[0];

            try (BufferedReader reader = new BufferedReader(new FileReader(geojsonStrFile0))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {

                    builder.append(line);
                }
                String geoJson = builder.toString();

                Map<String, Object> geojsonMap = JSONObject.parseObject(geoJson, Map.class);
                List<Map> features = (List<Map>) geojsonMap.get("features");

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
            } catch (Exception ex) {
                ex.printStackTrace();
            }
//            }


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


            List<Future<Boolean>> futures = new ArrayList<>();

            int fileSize = geojsonStrFiles.length;
            for (int k = 0; k < geojsonStrFiles.length; k++) {

                writerShp(file.getAbsolutePath(), geometryJSON, geojsonType, geojsonStrFiles[k], writer, fileSize, k);
            }

            for (Future<Boolean> future : futures) {
                future.get();
            }
            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.DAYS);
            log.info("输出写入 shp 文件完成，[ {} ]", file.getAbsolutePath());
            writer.close();
            ds.dispose();
            //生成编码文件
            generateCpgFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writerShp(String shapePath, GeometryJSON geometryJSON, String geojsonType, File geojsonStrFile1, FeatureWriter<SimpleFeatureType, SimpleFeature> writer, int fileSize, int k) {
        File geojsonStrFile = geojsonStrFile1;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(geojsonStrFile))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            String geoJson = builder.toString();

            Map<String, Object> geojsonMap = JSONObject.parseObject(geoJson, Map.class);
            List<Map> features = (List<Map>) geojsonMap.get("features");
            int f = k + 1;
            int index = 1;
            String name = geojsonStrFile.getName();
            for (Map oneGeojson : features) {

                log.info("开始输出文件[ {} -> {} / {} ], 要素[ {} / {} ]，写入shp文件，[ {} ]", name, f,
                        fileSize, index++, features.size(), shapePath);
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

        } catch (Exception ex) {
            ex.printStackTrace();
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
