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

    private ExecutorService executorService;

    public ExecutorService getExecutorService() {
        if (executorService != null) {
            return executorService;
        }
        int cpu = Runtime.getRuntime().availableProcessors();
        return getExecutorService(cpu);
    }

    public ExecutorService getExecutorService(int count) {
        if (executorService != null) {
            return executorService;
        }
        count = Math.max(2, count);

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("T_%d").build();
        int maxCpu = count << 2;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(count, maxCpu,
                10, TimeUnit.DAYS, new LinkedBlockingQueue<>(50), threadFactory, new TdtRejectPolicy());
        this.executorService = executor;
        return executor;
    }


}

