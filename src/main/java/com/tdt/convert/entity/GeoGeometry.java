package com.tdt.convert.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className GeoGeometry
 * @description
 * @date 2020-04-09 14:31
 **/

@Data
@ToString
public class GeoGeometry implements Serializable {

    private String type;

    private Object coordinates;
}
