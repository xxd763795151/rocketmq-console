package org.apache.rocketmq.console.interceptor.security;

import org.apache.rocketmq.console.model.User;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author xuxd
 * @Date 2020-12-18 19:20:46
 * @Description rocketmq-console-ng not admin user do not allow access and operator
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NotAdminDisable {

    int[] allow() default {User.ADMIN};
}
