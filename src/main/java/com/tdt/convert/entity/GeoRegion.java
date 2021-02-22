package com.tdt.convert.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className GeoRegion
 * @description
 * @date 2020-04-09 14:23
 **/

@Data
public class GeoRegion implements Serializable {


    private static final long serialVersionUID = -8950921047519178921L;


    private String type;

    private List<GeoFeatures> features;
    
}
