package com.tdt.convert.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tdt.convert.commons.TdtConst;
import com.tdt.convert.config.TdtConfig;
import com.tdt.convert.entity.coordinate.Children;
import com.tdt.convert.entity.coordinate.LngLat;
import com.tdt.convert.entity.coordinate.Tree;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className CoordinateUtil
 * @description
 * @date 2020-04-10 16:48
 **/

@Slf4j
@Component
public class CoordinateUtil {

    private int BATCH_COUNT = 100;


    @Autowired
    TdtConfig tdtConfig;


    public Object getBaiDuCoordinates(Object src) {
        if (src instanceof List) {

            Tree tree = new Tree();
            List<Children> childrenList = geoJson2Tree((List) src, tree.getChildren(), 0);
            tree.setChildren(childrenList);
            tree = tianDiTu2BaiDu(tree);
            return treeToGeoJson(tree);
        }
        return null;
    }

    /**
     * 天地图 点 转成 Tree
     *
     * @param coordinateList 点集合
     * @param childrenList   tree
     * @param index          索引
     * @return 转成的Tree结构
     */
    private List<Children> geoJson2Tree(List coordinateList, List<Children> childrenList, int index) {
        Children child = new Children();
        child.setIndex(index);
        childrenList.add(child);

        for (int i = 0; i < coordinateList.size(); i++) {

            Object coordinate = coordinateList.get(i);
            JSONArray jsonArray = JSON.parseArray(coordinate.toString());
            if (jsonArray.get(0) instanceof List) {
                geoJson2Tree((List) coordinate, child.getChildren(), i);
            } else {
                List<LngLat> lngLats = child.getLngLats();
                LngLat lngLat = new LngLat();
                lngLat.setIndex(i);
                lngLat.setLngLat((List) coordinate);
                lngLats.add(lngLat);

            }

        }
        return childrenList;
    }

    /**
     * tree转
     *
     * @param tree 转百度坐标系
     * @return 百度坐标系
     */
    private List<Object> treeToGeoJson(Tree tree) {
        return resursive(tree.getChildren());
    }

    private List<Object> resursive(List<Children> nodes) {

        List<Object> lngLatFlats = new LinkedList<>();
        for (Children node : nodes) {
            List<LngLat> lngLats = node.getLngLats();
            List<Children> children = node.getChildren();

            List<Object> objects = new LinkedList<>();
            objects.addAll(lngLats);
            objects.addAll(children);


            Stream<Object> sorted = objects.stream().sorted((o1, o2) -> {
                int index1, index2;
                if (o1 instanceof LngLat) {
                    index1 = ((LngLat) o1).getIndex();
                } else {
                    index1 = ((Children) o1).getIndex();
                }

                if (o2 instanceof LngLat) {
                    index2 = ((LngLat) o2).getIndex();
                } else {
                    index2 = ((Children) o2).getIndex();
                }
                return index1 - index2;

            });

            sorted.forEach(item -> {
                List<Object> lngLatFlat;

                if (item instanceof Children) {
                    Children itemChildren = (Children) item;
                    List<Children> itemChildrenList = itemChildren.getChildren();
                    if (itemChildrenList.size() > 0) {
                        lngLatFlat = resursive(Collections.singletonList(itemChildren));
                    } else {
                        lngLatFlat = ((Children) item).getLngLats().stream().map(LngLat::getLngLat).collect(Collectors.toList());
                    }
                } else {
                    LngLat lngLat = (LngLat) item;
                    lngLatFlat = new LinkedList<>(lngLat.getLngLat());
                }
                lngLatFlats.add(lngLatFlat);
            });
        }
        return lngLatFlats;
    }

    /**
     * 天地图 tree转百度Tree
     *
     * @param tree 天地图tree
     * @return 百度tree
     */
    private Tree tianDiTu2BaiDu(Tree tree) {

        //请求百度接口
//        resursiveTdt2BaiDuRemote(tree.getChildren());
        //本地算法转换
        resursiveTdt2BaiDuLocal(tree.getChildren());
        return tree;
    }


    private List<Children> resursiveTdt2BaiDuLocal(List<Children> nodes) {
        for (Children node : nodes) {
            List<LngLat> lngLats = node.getLngLats();
            List<Children> children = node.getChildren();


            for (int i = 0; i < lngLats.size(); i++) {
                LngLat lngLat = lngLats.get(i);
                List<Object> lngLatTmpList = lngLat.getLngLat();
                //经度
                BigDecimal lng = (BigDecimal) lngLatTmpList.get(0);
                //纬度
                BigDecimal lat = (BigDecimal) lngLatTmpList.get(1);
                double[] lngLatArr = LngLonUtil.transform(lng.doubleValue(), lat.doubleValue(), tdtConfig.getType());
                lngLatTmpList.set(0, lngLatArr[0]);
                lngLatTmpList.set(1, lngLatArr[1]);
                lngLat.setLngLat(lngLatTmpList);

                lngLats.set(i, lngLat);
            }
            resursiveTdt2BaiDuLocal(children);
        }
        return nodes;
    }

    /**
     * 请求百度接口转换坐标
     *
     * @param nodes 接口数据
     * @return 结果
     */
    private List<Children> resursiveTdt2BaiDuRemote(List<Children> nodes) {
        for (Children node : nodes) {

            List<LngLat> lngLats = node.getLngLats();
            List<Children> children = node.getChildren();

            List<List<LngLat>> partitionList = Lists.partition(lngLats, BATCH_COUNT);
            for (int j = 0; j < partitionList.size(); j++) {
                List<LngLat> lngLatsList = partitionList.get(j);

                //100个坐标转换
                Object[] objects = lngLatsList.stream().map(lngLat -> {
                    List<Object> lngLatTmpList = lngLat.getLngLat();
                    return lngLatTmpList.get(0) + "," + lngLatTmpList.get(1);
                }).toArray();
                String str = StringUtils.join(objects, ";");
                //请求百度接口

                Object baiDuLngLat = getBaiDuLngLat(str);
                if (baiDuLngLat instanceof List) {
                    List baiDuLngLatList = (List) (baiDuLngLat);
                    for (int k = 0; k < baiDuLngLatList.size(); k++) {
                        String baiDuStr = baiDuLngLatList.get(k).toString();
                        JSONObject jsonObject = JSONObject.parseObject(baiDuStr);
                        Object x = jsonObject.get("x");
                        Object y = jsonObject.get("y");
                        LngLat lngLat = lngLats.get(j * BATCH_COUNT + k);
                        lngLat.setLngLat(Arrays.asList(x, y));
                    }
                }
            }
            resursiveTdt2BaiDuRemote(children);
        }
        return nodes;
    }

    /**
     * 请求百度接口
     *
     * @param lngLatStr 要转换的坐标
     * @return 转换结果
     */
    public Object getBaiDuLngLat(String lngLatStr) {
        StringBuilder urlBuilder = new StringBuilder(TdtConst.BAI_DU_CONVERT_URL_PREFIX)
                .append(lngLatStr).append(TdtConst.BAI_DU_CONVERT_URL_SUFFIX).append("BhiU8m91IB3xGjZp83zrEeZC0CXMUbm4");

        String jsonResult = HttpClientUtil.sendHttpGet(urlBuilder.toString());

        if (jsonResult == null) {
            throw new RuntimeException("百度请求中没有返回数据");
        }
        JSONObject jsonObject = JSON.parseObject(jsonResult);
        Object result = jsonObject.get("result");
        if (result != null) {
            return result;

        } else {
            log.error("请求百度接口异常，经纬度转换失败");
            throw new RuntimeException("请求百度接口异常,经纬度转换失败,百度原因:\t" + jsonResult);

        }
    }
}
