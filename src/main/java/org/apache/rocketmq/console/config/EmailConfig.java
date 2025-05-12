package org.apache.rocketmq.console.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-13 21:21:21
 * @description rocketmq-console-ng
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.mail")
public class EmailConfig {

    private String username;
}
