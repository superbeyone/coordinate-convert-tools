package com.tdt.convert.entity.coordinate;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className LngLat
 * @description
 * @date 2020-04-13 09:42
 **/

@Slf4j
@Data
@ToString
public class LngLat implements Serializable {

    private int index;

    private List<Object> lngLat;

}
