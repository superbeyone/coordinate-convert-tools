package com.tdt.convert.commons;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className TdtConst
 * @description 静态配置信息
 * @date 2020-04-09 12:04
 **/

public class TdtConst {

    /**
     * 文件字符编码
     */
    public static final String CHARSET_NAME = "GB2312";

    /**
     * 百度坐标转换请求前缀
     */
    public static final String BAI_DU_CONVERT_URL_PREFIX = "http://api.map.baidu.com/geoconv/v1/?coords=";
    /**
     * 百度坐标转换后缀
     */
    public static final String BAI_DU_CONVERT_URL_SUFFIX = "&from=1&to=5&ak=";
    
    /**
     * 忽略拷贝的属性配置
     */
    public static final String[] IGNORE_PROPERTIES = {"X", "Y", "XMAX", "YMAX", "XMIN", "YMIN"};

}
