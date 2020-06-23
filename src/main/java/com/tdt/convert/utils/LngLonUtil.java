package com.tdt.convert.utils;

/**
 * @author superbeyone
 */
public class LngLonUtil {

    private static double pi = 3.1415926535897932384626;
    private static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
    private static double a = 6378245.0;
    private static double ee = 0.00669342162296594323;


    /**
     * 转换
     *
     * @param lon  经度 x
     * @param lat  纬度 y
     * @param type 转换类型
     *             1: 84 转 百度
     *             2: 百度 转 84
     *             3: 84 转 高德
     *             4: 高德 转 84
     *             5: 百度 转 高德
     *             6: 高德 转 百度
     * @return {经度,纬度} 数组
     */
    public static double[] transform(double lon, double lat, int type) {
        switch (type) {
            //1: 84 转 百度
            case 1:
                return gps84_To_bd09(lon, lat);
            //2: 百度 转 84
            case 2:
                return bd09_To_gps84(lon, lat);
            //3: 84 转 高德
            case 3:
                return gps84_To_Gcj02(lon, lat);
            //4: 高德 转 84
            case 4:
                return gcj02_To_Gps84(lon, lat);
            //5: 百度 转 高德
            case 5:
                return gcj02_To_Bd09(lon, lat);
            //6: 高德 转 百度
            case 6:
                return gcj02_To_Bd09(lon, lat);
            default:
        }
        return new double[]{lon, lat};
    }


    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * @param lon 经度
     * @param lat 纬度
     * @return
     */
    private static double[] transformCommon(double lon, double lat) {
        if (outOfChina(lon, lat)) {
            return new double[]{lon, lat};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLon, mgLat};
    }


    /**
     * 84 ==》 高德
     *
     * @param lon 经度
     * @param lat 纬度
     * @return
     */
    private static double[] gps84_To_Gcj02(double lon, double lat) {
        if (outOfChina(lon, lat)) {
            return new double[]{lat, lon};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLon, mgLat};
    }

    /**
     * 高德 ==》 84
     *
     * @param lon 经度
     * @param lat 纬度
     * @return
     */
    private static double[] gcj02_To_Gps84(double lon, double lat) {
        double[] gps = transformCommon(lon, lat);
        double longitude  = lon * 2 - gps[1];
        double latitude = lat * 2 - gps[0];
        return new double[]{longitude , latitude};
    }

    /**
     * 高德 == 》 百度
     *
     * @param lon 经度
     * @param lat 纬度
     */
    private static double[] gcj02_To_Bd09(double lon, double lat) {
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
        double tempLon = z * Math.cos(theta) + 0.0065;
        double tempLat = z * Math.sin(theta) + 0.006;
        double[] gps = {tempLon, tempLat};
        return gps;
    }

    /**
     * 百度 == 》 高德
     *
     * @param lon 经度
     * @param lat 纬度
     */
    private static double[] bd09_To_Gcj02(double lon, double lat) {
        double x = lon - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        double tempLon = z * Math.cos(theta);
        double tempLat = z * Math.sin(theta);
        double[] gps = {tempLon, tempLat};
        return gps;
    }

    /**
     * 84 == 》 百度
     *
     * @param lon 经度
     * @param lat 纬度
     * @return
     */
    private static double[] gps84_To_bd09(double lon, double lat) {
        double[] gcj02 = gps84_To_Gcj02(lon, lat);
        double[] bd09 = gcj02_To_Bd09(gcj02[0], gcj02[1]);
        return bd09;
    }

    /**
     * 百度 == 》 84
     *
     * @param lat 纬度
     * @param lon 经度
     * @return
     */
    private static double[] bd09_To_gps84(double lon, double lat) {
        double[] gcj02 = bd09_To_Gcj02(lon, lat);
        double[] gps84 = gcj02_To_Gps84(gcj02[0], gcj02[1]);
        //保留小数点后六位  
        gps84[0] = retain6(gps84[0]);
        gps84[1] = retain6(gps84[1]);
        return gps84;
    }

    /**
     * 保留小数点后六位
     *
     * @param num
     * @return
     */
    private static double retain6(double num) {
        String result = String.format("%.6f", num);
        return Double.valueOf(result);
    }

    /**
     * 判断是否在中国
     *
     * @param lon 经度
     * @param lat 纬度
     * @return
     */
    private static boolean outOfChina(double lon, double lat) {
        if (lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271) {
            return true;
        }
        return false;
    }
}

