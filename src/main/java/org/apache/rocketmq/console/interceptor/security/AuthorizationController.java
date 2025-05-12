package org.apache.rocketmq.console.interceptor.security;

import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.util.WebUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author xuxd
 * @Date 2020-12-21 10:13:15
 * @Description rocketmq-console-ng
 **/
@Component
public class AuthorizationController implements ApplicationContextAware {

    private Map<String, int[]> aclCache = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = handlerMapping.getHandlerMethods();
        handlerMethodMap.values().stream().forEach(handlerMethod -> {
            Class clazz = handlerMethod.getBeanType();
            int[] allow = null;
            if (clazz.isAnnotationPresent(NotAdminDisable.class)) {
                NotAdminDisable notAdminDisable = (NotAdminDisable) clazz.getDeclaredAnnotation(NotAdminDisable.class);
                allow = notAdminDisable.allow();
            }
            Method method = handlerMethod.getMethod();
            if (method.isAnnotationPresent(NotAdminDisable.class)) {
                NotAdminDisable notAdminDisable = method.getDeclaredAnnotation(NotAdminDisable.class);
                allow = notAdminDisable.allow();
            }
            if (allow != null) {
                aclCache.put(handlerMethod.toString(), allow);
            }
        });
    }

    public boolean isAllow(String handlerMethod) {
        User user = WebUtil.getLoginInfo().getUser();
        if (user.isAdmin()) {
            return true;
        }
        int role = user.getRole().getType();
        if (!aclCache.containsKey(handlerMethod)) {
            return true;
        }
        int[] allowRoles = aclCache.get(handlerMethod);
        for (int allow : allowRoles) {
            if (role == allow) {
                return true;
            }
        }
        return false;
    }
}
