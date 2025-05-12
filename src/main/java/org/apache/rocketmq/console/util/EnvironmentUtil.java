package org.apache.rocketmq.console.util;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @Author xuxd
 * @Date 2020-12-07 11:14:46
 * @Description environment util
 **/
@Component
public class EnvironmentUtil implements EnvironmentAware {

    public static Environment environment = null;

    @Override
    public void setEnvironment(Environment environment) {
        EnvironmentUtil.environment = environment;
    }

    public static String getProperty(String key) {
        return environment.getProperty(key);
    }

    public static String getActiveEnvironment() {
        return getProperty("spring.profiles.active");
    }
}
