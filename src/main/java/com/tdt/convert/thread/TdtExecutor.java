package com.tdt.convert.thread;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;


/**
 * @author Mr.superbeyone
 * @project online-data-manager
 * @className TdtExecutorBean
 * @description
 * @date 2019-04-12 16:24
 **/
@Component
public class TdtExecutor {

    public ExecutorService getExecutorService() {
        int cpu = Runtime.getRuntime().availableProcessors();
        return getExecutorService(cpu);
    }

    public ExecutorService getExecutorService(int count) {
        count = Math.max(2, count);

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("T_%d").build();
        return new ThreadPoolExecutor(count, 2 * count,
                10, TimeUnit.HOURS, new LinkedBlockingQueue<>(2 * count), threadFactory, new TdtRejectPolicy());
    }


}

