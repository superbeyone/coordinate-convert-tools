package com.tdt.convert.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className Region
 * @description
 * @date 2020-04-09 10:24
 **/

@Data
@ToString
public class Region implements Serializable {


    private static final long serialVersionUID = -4405484538737089793L;

    /**
     * 行政区划代码
     */
    private String gb;

    /**
     * 中文名称
     */
    private String name;

    /**
     * 英文名称
     */
    private String ename;
    /**
     * 英文名称
     */
    private String abename;

    /**
     * 纬度
     */
    private Double y;

    /**
     * 经度
     */
    private Double x;

    /**
     * 最大经度
     */
    private Double xmax;

    /**
     * 最大纬度
     */
    private Double ymax;
    
    /**
     * 最小经度
     */
    private Double xmin;

    /**
     * 最小纬度
     */
    private Double ymin;

    /**
     * 行政区划边界信息
     */
    private String the_geom;

    /**
     * 父级名称
     */
    private String asc_p;

    /**
     * 父级Gb
     */
    private String asc_p_gb;

    /**
     * 名称缩写
     */
    private String abcname;


}
