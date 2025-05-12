package org.apache.rocketmq.console.support;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-01-23 14:14:45
 * @description rocketmq-console-ng
 **/
public class CommonExecutors {

    public static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(2,
        40,
        1,
        TimeUnit.MINUTES,
        new ArrayBlockingQueue<>(100), r -> {
        Thread thread = new Thread(r);
        if (!thread.isDaemon()) {
            thread.setDaemon(true);
        }
        thread.setName("rmq-console-worker");
        return thread;
    }, new ThreadPoolExecutor.CallerRunsPolicy());

    public static void execute(Runnable runnable) {
        EXECUTOR_SERVICE.execute(runnable);
    }

}
