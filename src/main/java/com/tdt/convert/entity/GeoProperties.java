package com.tdt.convert.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className GeoProperties
 * @description
 * @date 2020-04-09 14:28
 **/

@Data
public class GeoProperties implements Serializable {

    @JSONField(name = "CNAME")
    private String CNAME;

    @JSONField(name = "ABCNAME")
    private String ABCNAME;

    @JSONField(name = "ENAME")
    private String ENAME;

    @JSONField(name = "ABENAME")
    private String ABENAME;

    @JSONField(name = "GB")
    private String GB;

    @JSONField(name = "X")
    private double X;

    @JSONField(name = "Y")
    private double Y;

    @JSONField(name = "ASC_P")
    private String ASC_P;

    @JSONField(name = "ASC_P_GB")
    private String ASC_P_GB;

    @JSONField(name = "XMAX")
    private double XMAX;

    @JSONField(name = "YMAX")
    private double YMAX;

    @JSONField(name = "XMIN")
    private double XMIN;

    @JSONField(name = "YMIN")
    private double YMIN;

    @JSONField(name = "ASC_D_GB")
    private String ASC_D_GB;

    @JSONField(name = "ASC_D")
    private String ASC_D;

    @Override
    public String toString() {
        return "GeoProperties{" +
                "CNAME='" + CNAME + '\'' +
                ", ABCNAME='" + ABCNAME + '\'' +
                ", ENAME='" + ENAME + '\'' +
                ", ABENAME='" + ABENAME + '\'' +
                ", GB='" + GB + '\'' +
                ", X=" + X +
                ", Y=" + Y +
                ", ASC_P='" + ASC_P + '\'' +
                ", ASC_P_GB='" + ASC_P_GB + '\'' +
                ", XMAX=" + XMAX +
                ", YMAX=" + YMAX +
                ", XMIN=" + XMIN +
                ", YMIN=" + YMIN +
                ", ASC_D_GB='" + ASC_D_GB + '\'' +
                ", ASC_D='" + ASC_D + '\'' +
                '}';
    }
}
