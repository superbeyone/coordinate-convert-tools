package com.tdt.convert.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tdt.convert.config.TdtConfig;
import com.tdt.convert.entity.GeoRegion;
import com.tdt.convert.service.GeoJsonReaderService;
import com.tdt.convert.service.ShapeBigReaderService;
import com.tdt.convert.thread.TdtExecutor;
import com.tdt.convert.utils.FileUtil;
import com.tdt.convert.utils.TimeUtil;
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
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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

    private static int SINGLE_FILE_COUNT = 2000;

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
            long start = System.currentTimeMillis();
            System.out.println();
            System.out.println();
            log.info("===================== Shape ===============================");
            log.info("||\t\t\t开始处理第 [ {} / {} ]个任务\t\t\t||", num, shpList.size());
            log.info("===================== Shape ===============================\n\n");
            String absolutePath = file.getAbsolutePath();
            if (StringUtils.contains(input, "/")) {
                input = StringUtils.replace(input, "/", "\\");
            }
            String filePath = StringUtils.substringAfter(absolutePath, input);

            File outPutRoot_1_2 = new File(outPutRoot, StringUtils.substringBefore(file.getName(), "."));

            File geoJsonDir = new File(outPutRoot_1_2, "step_1_shp2geojson_dir");
            File convertedGeoJsonDir = new File(outPutRoot_1_2, "step_2_converted_geojson_dir");
            File convertShpDir = new File(outPutRoot, "step_3_convert_shp_dir");

            geoJsonDir.mkdirs();
            convertedGeoJsonDir.mkdirs();
            convertShpDir.mkdirs();

            File outputShapeFile = new File(convertShpDir, filePath);
            convertShp2GeoJson(file, charset, geoJsonDir);


            convertGeoJson(shpList.size(), num, geoJsonDir, convertedGeoJsonDir);

            //获取shape属性头信息
            List<AttributeDescriptor> attributeDescriptorList = getShapeFileHeader(file);
            //输出shape
            geoJson2Shape(convertedGeoJsonDir, outputShapeFile, attributeDescriptorList);
            //删除临时文件
            if (tdtConfig.isDelTempFile()) {
                ExecutorService executorService = tdtExecutor.getExecutorService();
                executorService.execute(() -> {
                    log.info("异步删除临时文件 [ {} ]", outPutRoot_1_2);
                    FileUtil.delAllFile(outPutRoot_1_2);
                });
            }

            System.out.println();
            System.out.println();
            log.info("================== Shape ========================");
            log.info("||\t\t\t处理完成第 [ {} / {} ]个任务\t\t\t||", num, shpList.size());
            log.info("||\t\t\t文件[ {} ]] \t\t\t||", file.getAbsolutePath());
            log.info("||\t\t\t耗时[ {} ] \t\t\t||", TimeUtil.timeDiffer(System.currentTimeMillis(), start));
            log.info("================= Shape =========================\n\n");

        }
    }

    private void convertGeoJson(int shpFileCount, int num, File geoJsonDir, File convertedGeoJsonDir) {

        if (geoJsonDir.isDirectory()) {
            File[] srcGeoJsonFileArr = geoJsonDir.listFiles();
            if (srcGeoJsonFileArr != null && srcGeoJsonFileArr.length > 0) {
                for (int i = 0; i < srcGeoJsonFileArr.length; i++) {
                    File srcGeoJsonFile = srcGeoJsonFileArr[i];
                    StringBuilder builder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(srcGeoJsonFile));
                         OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(new File(convertedGeoJsonDir, srcGeoJsonFile.getName())))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        log.info("开始坐标转换 [ {} / {} ]，文件[ {} ]", i + 1, srcGeoJsonFileArr.length, srcGeoJsonFile.getAbsolutePath());
                        GeoRegion geoRegion = JSONObject.parseObject(builder.toString(), GeoRegion.class);
                        String resultJson = geoJsonReaderService.convertGeoJson(geoRegion, srcGeoJsonFile.getAbsolutePath(), num, shpFileCount);
                        outputStream.write(resultJson.getBytes());
                        outputStream.flush();
                        log.info("坐标转换完成 [ {} / {} ]，文件[ {} ]", i + 1, srcGeoJsonFileArr.length, srcGeoJsonFile.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void convertShp2GeoJson(File file, String charset, File geoJsonDir) {
        log.info("Shape文件[ {} ] 转 GeoJson 开始", file.getAbsolutePath());

        try {

            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(file.toURI().toURL());
            shapefileDataStore.setCharset(Charset.forName(charset));

            ContentFeatureSource featureSource = shapefileDataStore.getFeatureSource();

            ContentFeatureCollection features = featureSource.getFeatures();
            FeatureJSON featureJSON = new FeatureJSON(new GeometryJSON(DECIMALS));
            SimpleFeatureIterator iterator = features.features();
            List<SimpleFeature> simpleFeatures = new LinkedList<>();
            SimpleFeatureType featureType = null;
            AtomicInteger index = new AtomicInteger(0);
            int count = features.size() / SINGLE_FILE_COUNT;
            count = features.size() % SINGLE_FILE_COUNT == 0 ? count : count + 1;

            ExecutorService executorService = tdtExecutor.getExecutorService();
            List<Future<Boolean>> futures = new ArrayList<>();
            while (iterator.hasNext()) {
                if (simpleFeatures.size() == SINGLE_FILE_COUNT) {
                    int finalCount = count;
                    SimpleFeatureType finalFeatureType = featureType;
                    List<SimpleFeature> finalSimpleFeatures = simpleFeatures;
                    Future<Boolean> future = executorService.submit(() -> {
                        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(new File(geoJsonDir, index.getAndIncrement() + ".geojson")))) {
                            log.info("拆分文件[ {} / {} ]，文件[ {} ]", index.get(), finalCount, file.getAbsolutePath());
                            SimpleFeatureCollection collection = new ListFeatureCollection(finalFeatureType, finalSimpleFeatures);
                            featureJSON.writeFeatureCollection(collection, outputStream);
                            outputStream.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    });
                    futures.add(future);
                    simpleFeatures = new LinkedList<>();
                }
                SimpleFeature feature = iterator.next();
                featureType = feature.getFeatureType();
                simpleFeatures.add(feature);
            }
            SimpleFeatureCollection collection = new ListFeatureCollection(featureType, simpleFeatures);
            //最后一批次
            if (simpleFeatures.size() > 0) {
                int finalCount1 = count;
                Future<Boolean> future = executorService.submit(() -> {
                    try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(new File(geoJsonDir, index + ".geojson")))) {
                        log.info("拆分文件[ {} / {} ]，文件[ {} ]", index.get(), finalCount1, file.getAbsolutePath());
                        featureJSON.writeFeatureCollection(collection, outputStream);
                        outputStream.flush();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                });
                futures.add(future);
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        log.info("Shape文件[ {} ] 转 GeoJson 完成", file.getAbsolutePath());

    }

    /**
     * 获取Shp文件属性头信息
     * @param file shp文件
     * @return 属性头信息
     */
    private List<AttributeDescriptor> getShapeFileHeader(File file) {
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        try {
            ShapefileDataStore sds = (ShapefileDataStore) dataStoreFactory.createDataStore(file.toURI().toURL());

            sds.setCharset(Charset.forName("GB2312"));
            SimpleFeatureSource featureSource = sds.getFeatureSource();


            sds.dispose();
            return featureSource.getSchema().getAttributeDescriptors();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    /**
     * 获取空间类型
     *
     * @param attributeDescriptorList 头信息
     * @return 空间信息
     */
    private Class<?> getGeometryClass(List<AttributeDescriptor> attributeDescriptorList) {
        for (AttributeDescriptor attributeDescriptor : attributeDescriptorList) {
            AttributeType type = attributeDescriptor.getType();
            if (type instanceof GeometryTypeImpl) {
                GeometryTypeImpl geometryType = (GeometryTypeImpl) type;
                return geometryType.getBinding();
            }
        }
        return Geometry.class;
    }

    /**
     * 输出Shape文件
     *
     * @param convertedGeoJsonDir     geoJson文件夹
     * @param file                    shp文件
     * @param attributeDescriptorList 头信息
     */
    private void geoJson2Shape(File convertedGeoJsonDir, File file, List<AttributeDescriptor> attributeDescriptorList) {

        try {
            log.info("开始初始化shape文件");
            String fileName = file.getName();
            if (!StringUtils.endsWithIgnoreCase(fileName, ".shp")) {
                file = new File(file.getParentFile(), fileName + ".shp");
            }
            GeometryJSON geometryJSON = new GeometryJSON(DECIMALS);
            //创建shape文件对象
            Map<String, Serializable> params = new HashMap<>();
            params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
            ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

            //定义图形信息和属性信息
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setCRS(DefaultGeographicCRS.WGS84);
            tb.setName("shapefile");

            tb.addAll(attributeDescriptorList);
            //获取空间类型
            Class<?> geometryTypeClass = getGeometryClass(attributeDescriptorList);


            File[] geojsonStrFiles = convertedGeoJsonDir.listFiles();
            ds.setCharset(Charset.forName("GB2312"));
            ds.createSchema(tb.buildFeatureType());
            log.info("shape文件初始化完成");
            //设置Writer
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.
                    getFeatureWriter(ds.getTypeNames()[0], Transaction.AUTO_COMMIT);

            log.info("开始输出写入 shp 文件，[{}]", file.getAbsolutePath());

            for (int k = 0; k < geojsonStrFiles.length; k++) {
                File geojsonStrFile = geojsonStrFiles[k];

                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(geojsonStrFile))) {
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        builder.append(line);
                    }
                    String geoJson = builder.toString();
                    log.info("合并 GeoJson [ {} -> {} ]", geojsonStrFile.getName(), file.getName());
                    Map<String, Object> geojsonMap = JSONObject.parseObject(geoJson, Map.class);
                    List<Map> features = (List<Map>) geojsonMap.get("features");
                    int f = k + 1;
                    int index = 1;
                    String name = geojsonStrFile.getName();
                    for (Map oneGeojson : features) {

                        if (log.isDebugEnabled()) {
                            log.info("开始输出文件[ {} -> {} / {} ], 要素[ {} / {} ]，写入shp文件，[ {} ]", name, f, geojsonStrFiles.length, index++, features.size(), file.getAbsolutePath());
                        }
                        Map<String, Object> attributes = (Map<String, Object>) oneGeojson.get("properties");
                        String strFeature = JSONObject.toJSONString(oneGeojson);
                        Reader reader = new StringReader(strFeature);
                        SimpleFeature feature = writer.next();

                        if (geometryTypeClass == Point.class) {
                            feature.setAttribute("the_geom", geometryJSON.readPoint(reader));
                        } else if (geometryTypeClass == MultiPoint.class) {
                            feature.setAttribute("the_geom", geometryJSON.readMultiPoint(reader));
                        } else if (geometryTypeClass == LineString.class) {
                            feature.setAttribute("the_geom", geometryJSON.readLine(reader));
                        } else if (geometryTypeClass == MultiLineString.class) {
                            feature.setAttribute("the_geom", geometryJSON.readMultiLine(reader));
                        } else if (geometryTypeClass == Polygon.class) {
                            feature.setAttribute("the_geom", geometryJSON.readPolygon(reader));
                        } else if (geometryTypeClass == MultiPolygon.class) {
                            feature.setAttribute("the_geom", geometryJSON.readMultiPolygon(reader));
                        } else {
                            feature.setAttribute("the_geom", geometryJSON.read(reader));
                        }

                        for (String key : attributes.keySet()) {
                            feature.setAttribute(key, attributes.get(key));
                        }

                    }
                    writer.write();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
