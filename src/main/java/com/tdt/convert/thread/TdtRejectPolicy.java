package com.tdt.convert.thread;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Mr.superbeyone
 * @project tile-reimport-tool
 * @className TdtRejectPolicy
 * @description
 * @date 2020-10-14 19:05
 **/

public class TdtRejectPolicy implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (!executor.isShutdown()) {
            while (executor.getQueue().remainingCapacity() == 0) {
            }
            executor.execute(r);

        }
    }
}
