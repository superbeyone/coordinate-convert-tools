package com.tdt.convert.thread;

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
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        return new ThreadPoolExecutor(count, count + 4,
                1, TimeUnit.HOURS, new LinkedBlockingQueue<>(2 * count), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }


}

