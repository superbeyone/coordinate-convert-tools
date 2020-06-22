package com.tdt.convert.utils.coding;

import java.io.File;


/**
 * @author Mr.superbeyone
 */
public class EncodingDetect {

    /**
     * 得到文件的编码	
     *
     * @param filePath 文件路径
     * @return 文件的编码
     */
    public static String getFileEncode(String filePath) {
        BytesEncodingDetect s = new BytesEncodingDetect();
        String fileCode = BytesEncodingDetect.javaname[s.detectEncoding(new File(filePath))];
        return fileCode;
    }

    public static String getFileEncode(File file) {
        BytesEncodingDetect s = new BytesEncodingDetect();
        String fileCode = BytesEncodingDetect.javaname[s.detectEncoding(file)];
        return fileCode;
    }
}
