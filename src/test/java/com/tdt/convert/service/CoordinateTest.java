package com.tdt.convert.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.tdt.convert.entity.coordinate.Children;
import com.tdt.convert.entity.coordinate.LngLat;
import com.tdt.convert.entity.coordinate.Tree;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className CoordinateTest
 * @description
 * @date 2020-04-13 09:46
 **/

public class CoordinateTest {


    private int BATCH_COUNT = 100;

    @Test
    public void testCoordinate() {
        String coords = "[[[[1,2],[3,4],[11,22],[33,44]]],[[[5,6],[7,8],[[10,11],[12,13]]]],[[[15,16],[51,61]]],[[17,18],[19,20]]]";
//        String c2 = "[[[[1,2],[3,4],[11,22],[33,44]]],[[[5,6],[7,8],[[10,11],[12,13]]]],[[[15,16],[51,61]]],[[17,18],[19,20]]]";
        JSONArray jsonArray = JSON.parseArray(coords);
        Tree tree = new Tree();
        List<Children> childrenList = geoJson2Tree(jsonArray, tree.getChildren(), 0);
        tree.setChildren(childrenList);
//        System.out.println(JSON.toJSONString(tree, true));

//        System.out.println();
//        System.out.println();
//        System.out.println();

        tree = tianDiTu2BaiDu(tree);
//        System.out.println("请求百度接口后获取的值");
//        System.out.println();
//
//        System.out.println(JSON.toJSONString(tree));

        System.out.println();
        System.out.println();

        List<Object> objectList = treeToGeoJson(tree);
        String treeJson = JSON.toJSONString(objectList);
        System.out.println("转之前的值");
        System.out.println(coords);
        System.out.println("转之后的值");
        System.out.println(treeJson);


        System.out.println();
    }


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

    private Tree tianDiTu2BaiDu(Tree tree) {

        resursiveTdt2BaiDu(tree.getChildren());
        return tree;
    }

    private List<Children> resursiveTdt2BaiDu(List<Children> nodes) {
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
                        String[] split = StringUtils.split(baiDuStr, ",");
                        int x = Integer.parseInt(split[0]);
                        int y = Integer.parseInt(split[1]);
//                        JSONObject jsonObject = JSONObject.parseObject(baiDuStr);
//                        Integer x = (Integer) jsonObject.get("x");
//                        Integer y = (Integer) jsonObject.get("y");
                        LngLat lngLat = lngLatsList.get(j * BATCH_COUNT + k);
                        lngLat.setLngLat(Arrays.asList(x, y));
                    }
                }
            }
            resursiveTdt2BaiDu(children);
        }
        return nodes;
    }


    private Object getBaiDuLngLat(String lngLatStr) {
        List<String> list = new ArrayList<>();
        String[] lngLatArr = StringUtils.split(lngLatStr, ";");
        for (String s : lngLatArr) {
            String[] lngLatArray = StringUtils.split(s, ",");
            String str = (Integer.parseInt(lngLatArray[0]) + 1) + "," + (Integer.parseInt(lngLatArray[1]) + 1);
            list.add(str);
        }
        return list;
    }


}
