/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.console.support;

import org.apache.rocketmq.console.aspect.admin.annotation.OriginalControllerReturnValue;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.UserInfo;
import org.apache.rocketmq.console.util.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.annotation.Annotation;

@ControllerAdvice(basePackages = "org.apache.rocketmq.console")
public class GlobalRestfulResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private Logger logger = LoggerFactory.getLogger(GlobalRestfulResponseBodyAdvice.class);

    @Autowired
    private RMQConfigure rmqConfigure;

    @Override
    public Object beforeBodyWrite(
            Object obj, MethodParameter methodParameter, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        Annotation originalControllerReturnValue = methodParameter.getMethodAnnotation(OriginalControllerReturnValue.class);
        if (originalControllerReturnValue != null) {
            return obj;
        }
        JsonResult value;
        if (obj instanceof JsonResult) {
            value = (JsonResult) obj;
        } else {
            value = new JsonResult(obj);
        }
        if (rmqConfigure.isLoginRequired()) {
            ServletServerHttpRequest request = (ServletServerHttpRequest) serverHttpRequest;
            UserInfo userInfo = (UserInfo) WebUtil.getValueFromSession(request.getServletRequest(), WebUtil.USER_INFO);
            value.setRole(userInfo == null ? 0 : userInfo.getUser().getType());
        } else {
            value.setRole(1);
        }

        return value;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {

        return true;
    }

}
