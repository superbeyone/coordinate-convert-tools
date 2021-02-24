package com.tdt.convert.utils;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className TimeUtil
 * @description
 * @date 2021-02-24 17:52
 **/

public class TimeUtil {

    public static String timeDiffer(long end, long start) {
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long different = end - start;
        long d = different / daysInMilli;
        different = different % daysInMilli;

        long h = different / hoursInMilli;
        different = different % hoursInMilli;

        long m = different / minutesInMilli;
        different = different % minutesInMilli;

        long s = different / secondsInMilli;

        return d + " 天, " + h + " 时, " + m + " 分, " + s + " 秒";
    }
}
