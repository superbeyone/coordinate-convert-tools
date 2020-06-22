package com.tdt.convert.entity.coordinate;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className Tree
 * @description
 * @date 2020-04-13 11:01
 **/

@Data
@ToString
public class Tree implements Serializable {

    private List<Children> children = new LinkedList<>();
}
