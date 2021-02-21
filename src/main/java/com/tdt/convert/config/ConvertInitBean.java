package com.tdt.convert.config;

import com.tdt.convert.service.ConvertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Mr.superbeyone
 * @project coordinate-convert-tools
 * @className ConvertInitBean
 * @description
 * @date 2020-04-09 10:16
 **/
@Slf4j
@Component
public class ConvertInitBean implements InitializingBean {


    @Autowired
    ConvertService convertService;

    @Override
    public void afterPropertiesSet() throws Exception {
        convertService.convertFile();
    }
}
