package org.apache.rocketmq.console.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @Author xuxd
 * @Date 2020-12-07 14:47:36
 * @Description spring bean util
 **/
@Component
public class BeanUtil implements ApplicationContextAware, EnvironmentAware {

    private static ApplicationContext applicationContext;

    private static Environment environment;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BeanUtil.applicationContext = applicationContext;
    }

    public static  <T> T getBean(Class<T> t) {
        return applicationContext.getBean(t);
    }

    public static Environment getEnvironment() {
        return environment;
    }

    @Override public void setEnvironment(Environment environment) {
        BeanUtil.environment = environment;
    }
}
