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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.console.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.LoginInfo;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.UserInfo;
import org.apache.rocketmq.console.model.request.UpdateUserRequest;
import org.apache.rocketmq.console.service.UserService;
import org.apache.rocketmq.console.util.CommonUtil;
import org.apache.rocketmq.console.util.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/login")
public class LoginController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private RMQConfigure configure;

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/check.query", method = RequestMethod.GET)
    @ResponseBody
    public Object check(HttpServletRequest request) {
        LoginInfo loginInfo = new LoginInfo();

        loginInfo.setLogined(WebUtil.getValueFromSession(request, WebUtil.USER_NAME) != null);
        loginInfo.setLoginRequired(configure.isLoginRequired());

        return loginInfo;
    }

    @RequestMapping(value = "/login.do", method = RequestMethod.POST)
    @ResponseBody
    public Object login(@RequestParam("username") String username,
                        @RequestParam(value = "password") String password,
                        HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
        logger.info("user:{} login", username);
        User user = userService.queryByUsernameAndPassword(username, password);

        if (user == null) {
            throw new IllegalArgumentException("Bad username or password!");
        } else {
            user.setPassword(null);
            UserInfo userInfo = WebUtil.setLoginInfo(request, response, user);
            WebUtil.setSessionValue(request, WebUtil.USER_INFO, userInfo);
            WebUtil.setSessionValue(request, WebUtil.USER_NAME, username);
            userInfo.setSessionId(WebUtil.getSessionId(request));

            return userInfo;
        }
    }

    @RequestMapping(value = "/logout.do", method = RequestMethod.POST)
    @ResponseBody
    public Object logout(HttpServletRequest request) {
        WebUtil.removeSession(request);

        return Boolean.TRUE;
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    @ResponseBody
    public Object user(HttpServletRequest request) {
        if (!configure.isLoginRequired()) {
            UserInfo userInfo = new UserInfo();
            User user = new User("admin", "", 1);
            userInfo.setUser(user);
            return userInfo;
        }
        return WebUtil.getValueFromSession(request, WebUtil.USER_INFO);
    }

    @PostMapping("/register.do")
    @ResponseBody
    public Object autoRegisterAccount(@RequestBody UpdateUserRequest user, HttpServletRequest request) {
        logger.info("param: {}", user);
        Objects.requireNonNull(user.getEmail(), "Email is null");
        Objects.requireNonNull(user.getItem(), "Item is null");
        HttpSession session = request.getSession();
        if (session == null) {
            throw new IllegalStateException("没有session信息");
        }
        String clientFlag = null;
        try {
            clientFlag = CommonUtil.md5(session.getId());
        } catch (Exception e) {
            throw new IllegalArgumentException("获取客户端标识失败", e);
        }
        return userService.registerAccount(user, clientFlag);
    }

    @PostMapping("/forget.do")
    @ResponseBody
    public Object forgetPassword(@RequestBody UpdateUserRequest user, HttpServletRequest request) {
        logger.info("param: {}", user);
        Objects.requireNonNull(user.getEmail(), "Email is null");
        HttpSession session = request.getSession();
        if (session == null) {
            throw new IllegalStateException("没有session信息");
        }
        String clientFlag = null;
        try {
            clientFlag = CommonUtil.md5(session.getId());
        } catch (Exception e) {
            throw new IllegalArgumentException("获取客户端标识失败", e);
        }
        return userService.forgetPassword(user, clientFlag);
    }

    @GetMapping("/info/all.query")
    @ResponseBody
    public Object getUserInfo() {
        Map<String, Object> res = new HashMap<>();
        res.put("role", userService.selectRoles());
        res.put("item", userService.selectItems());
        return res;
    }
}
