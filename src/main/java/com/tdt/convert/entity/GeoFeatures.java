package com.tdt.convert.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className GeoFeatures
 * @description
 * @date 2020-04-09 14:25
 **/

@Data
public class GeoFeatures implements Serializable {

    private static final long serialVersionUID = 5862872820518932058L;

    private String type;

    private String id;

    private Object properties;

    private GeoGeometry geometry;

    
}
