package org.apache.rocketmq.console.common.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-12-24 14:57:36
 **/
@Configuration
public class AsyncUtilProvider {

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
    }
}
