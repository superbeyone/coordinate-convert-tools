package com.tdt.convert.entity.coordinate;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className Children
 * @description
 * @date 2020-04-13 09:43
 **/

@Data
@ToString
public class Children implements Serializable {

    private Integer index;

    private List<LngLat> lngLats = new LinkedList<>();

    private List<Children> children = new LinkedList<>();

}
