package com.tdt.convert.config;

import com.tdt.convert.CoordinateConvertToolsApplication;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className TdtConfig
 * @description 配置信息
 * @date 2020-04-09 12:02
 **/

@Configuration
@ConfigurationProperties(value = "tdt.convert")
public class TdtConfig {

    /**
     * 输入路径
     */
    private String input;

    private boolean delTempFile;

    /**
     * 输出路径
     */
    private String output;

    /**
     * 字符编码
     */
    private String charset = "GB2312";

    /**
     * ############ type 参数说明 ############
     * #                                   #
     * #            1: 84 转 百度           #
     * #            2: 百度 转 84           #
     * #            3: 84 转 高德           #
     * #            4: 高德 转 84           #
     * #            5: 百度 转 高德          #
     * #            6: 高德 转 百度          #
     * #                                   #
     * #####################################
     */
    private int type;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        if (StringUtils.isBlank(input)) {
            this.input = getLocation() + "/input";
        } else {
            this.input = input;
        }
    }

    public boolean isDelTempFile() {
        return delTempFile;
    }

    public void setDelTempFile(boolean delTempFile) {
        this.delTempFile = delTempFile;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        if (StringUtils.isBlank(output)) {
            this.output = getLocation() + "/output";
        } else {
            this.output = output;
        }
    }

    public String getCharset() {
        return StringUtils.isBlank(charset) ? "GB2312" : charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private String getLocation() {
        String location = "";
        try {
            location = URLDecoder.decode(CoordinateConvertToolsApplication.class.getProtectionDomain().getCodeSource().getLocation().getFile(),
                    "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("获取路径失败：" + e.getMessage());
        }
        if (StringUtils.containsIgnoreCase(location, ".jar")) {
            location = StringUtils.substringBefore(location, ".jar");
            location = StringUtils.substringBeforeLast(location, "/");
        }
        String prefix = "file:/";
        if (StringUtils.startsWithIgnoreCase(location, prefix)) {
            return StringUtils.substringAfter(location, prefix);
        }
        return location;
    }
}
