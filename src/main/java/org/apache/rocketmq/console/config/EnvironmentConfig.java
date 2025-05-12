package org.apache.rocketmq.console.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-13 16:48:23
 * @description rocketmq-console-ng
 **/
@Configuration
@ConfigurationProperties(prefix = "console.environment")
@Data
public class EnvironmentConfig {

    private String account;

    private String resources;

    private List<String> multiApply;

}
