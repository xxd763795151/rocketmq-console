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

package org.apache.rocketmq.console.service.impl;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.console.common.ExpireCleanCache;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.config.RegisterConfig;
import org.apache.rocketmq.console.dao.BelongItemMapper;
import org.apache.rocketmq.console.dao.ItemMapper;
import org.apache.rocketmq.console.dao.RoleMapper;
import org.apache.rocketmq.console.dao.UserMapper;
import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.listener.event.ForgetPasswordEvent;
import org.apache.rocketmq.console.listener.event.RegisterAccountEvent;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.Role;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.UpdateUserRequest;
import org.apache.rocketmq.console.service.UserService;
import org.apache.rocketmq.console.support.JsonResult;
import org.apache.rocketmq.console.util.CommonUtil;
import org.apache.rocketmq.console.util.RandomUtil;
import org.apache.rocketmq.srvutil.FileWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    FileBasedUserInfoStore fileBasedUserInfoStore;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private ItemMapper itemMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private BelongItemMapper belongItemMapper;

    @Autowired
    private ApplicationContext applicationContext;

    private RegisterConfig registerConfig;

    private ExpireCleanCache<String, Object> emailCache, clientCache, forgetCache;

    private final long FORGET_INTERVAL;

    public UserServiceImpl(@Autowired RegisterConfig registerConfig) {
        this.registerConfig = registerConfig;
        this.emailCache = new ExpireCleanCache<>(registerConfig.getEmailInterval());
        this.clientCache = new ExpireCleanCache<>(registerConfig.getClientInterval());
        this.FORGET_INTERVAL = registerConfig.getEmailInterval() * 100;
        this.forgetCache = new ExpireCleanCache<>(FORGET_INTERVAL);
    }

    @Override
    public User queryByName(String name) {
        return fileBasedUserInfoStore.queryByName(name);
    }

    @Override
    public User queryByUsernameAndPassword(String username, String password) {
        User user = userMapper.selectByNameAndPass(username, password);
        if (user != null) {
            user.setType(user.getRole().getType());
        }
        return user;
    }

    @Override
    public List<Role> selectRoles() {
        return roleMapper.selectAll();
    }

    @Override
    public List<Item> selectItems() {
        return itemMapper.selectAll();
    }

    @Override
    public List<User> selectUserList(User user) {
        return userMapper.selectUserList(user);
    }

    @Override
    public int addUser(UpdateUserRequest user) {
        return userMapper.addUser(user);
    }

    @Override
    public int updateUser(UpdateUserRequest user) {
        return userMapper.updateUser(user);
    }

    @Override
    public int deleteUser(Long id) {
        return userMapper.deleteUser(id);
    }

    @Override
    public int addRole(Role role) {
        return roleMapper.addRole(role);
    }

    @Override
    public int updateRole(Role role) {
        return roleMapper.updateRole(role);
    }

    @Override
    public int addItem(Item item) {
        return itemMapper.addItem(item);
    }

    @Override
    public int updateItem(Item item) {
        return itemMapper.updateItem(item);
    }

    @Transactional
    @Override
    public int updateBelongItem(Map<String, Object> params) {
        Integer type = (Integer) params.get("type");
        if (type == 1) {
            // type: 1 == topic, 2== consume group
            belongItemMapper.deleteBy(params);
            return belongItemMapper.addBatchSingleType(params);
        } else {
            int res = 0;
            // add retry topic , dlq topic
            String name = (String) params.get("name");
            String retry = MixAll.RETRY_GROUP_TOPIC_PREFIX + name;
            String dlq = MixAll.DLQ_GROUP_TOPIC_PREFIX + name;
            belongItemMapper.deleteBy(params);
            res += belongItemMapper.addBatchSingleType(params);
            params.put("name", retry);
            params.put("type", 1);
            belongItemMapper.deleteBy(params);
            res += belongItemMapper.addBatchSingleType(params);
            params.put("name", dlq);
            belongItemMapper.deleteBy(params);
            res += belongItemMapper.addBatchSingleType(params);
            return res;
        }
    }

    @Override public List<User> selectNotifyUser() {
        return userMapper.selectNotifyUser();
    }

    @Override public JsonResult<String> registerAccount(UpdateUserRequest request, String clientFlag) {
        String resultMessage = "success";
        String email = request.getEmail();

        if (registerConfig.isClientIntervalCheck()) {
            if (clientCache.contains(clientFlag)) {
                Long second = registerConfig.getClientInterval() / 1000;
                return new JsonResult<String>(-9999, second + "秒内禁止注册操作");
            }
            clientCache.put(clientFlag, clientFlag);
        }

        if (registerConfig.isEmailIntervalCheck()) {
            if (emailCache.contains(email)) {
                Long second = registerConfig.getEmailInterval() / 1000;
                return new JsonResult<String>(-9999, second + "秒内已使用该邮箱进行注册操作");
            }
        }

        String name = email.split("@")[0];

        Optional<User> user = userMapper.selectUserByName(name);
        if (user.isPresent()) {
            return new JsonResult<>(-9999, "用户" + name + "已经被注册，注册邮箱：" + user.get().getEmail());
        }

        user = userMapper.selectUserByEmail(email);
        if (user.isPresent()) {
            return new JsonResult<>(-9999, "该邮箱已注册，注册的用户名为：" + user.get().getName());
        }

        long roleId = roleMapper.selectByType(User.ORDINARY).get(0).getId();
        String pass = RandomUtil.randomString(10);

        request.setName(name);
        request.setPassword(pass);
        request.setRole(roleId);

        userMapper.addUser(request);
        emailCache.put(email, email);
        LOGGER.info("registerAccount: {}", request);

        applicationContext.publishEvent(new RegisterAccountEvent(this, request));
        return new JsonResult<>(resultMessage);
    }

    @Override public Optional<User> selectUserByID(Long id) {
        return userMapper.selectUserByID(id);
    }

    @Override public Object forgetPassword(UpdateUserRequest request, String clientFlag) {

        if (registerConfig.isClientIntervalCheck()) {
            if (clientCache.contains(clientFlag)) {
                Long second = registerConfig.getClientInterval() / 1000;
                return new JsonResult<String>(-9999, second + "秒内禁止操作");
            }
            clientCache.put(clientFlag, clientFlag);
        }

        Optional<User> optionalUser = userMapper.selectUserByEmail(request.getEmail());
        if (!optionalUser.isPresent()) {
            return new JsonResult<String>(-9999, "邮箱: " + request.getEmail() + "未绑定账号，请联系管理员处理");
        }

        User user = optionalUser.get();
        if (registerConfig.isEmailIntervalCheck()) {
            if (forgetCache.contains(user.getEmail())) {
                Long second = FORGET_INTERVAL / 1000;
                String last = (String) forgetCache.get(user.getEmail());
                return new JsonResult<String>(-9999, "该邮箱最近一次忘记密码操作的时间为：" + last + ", 至少间隔：" + second + "秒，才能再操作");
            }
            forgetCache.put(user.getEmail(), CommonUtil.formatCurrentTime());
        }

        applicationContext.publishEvent(new ForgetPasswordEvent(this, user));
        return new JsonResult<String>(0, "success");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
//        if (configure.isEnableDashBoardCollect()) {
//            fileBasedUserInfoStore = new FileBasedUserInfoStore(configure);
//        }
    }

    /*packaged*/ static class FileBasedUserInfoStore {
        private final Logger log = LoggerFactory.getLogger(this.getClass());
        private static final String FILE_NAME = "users.properties";

        private String filePath;
        private final Map<String, User> userMap = new ConcurrentHashMap<>();

        public FileBasedUserInfoStore(RMQConfigure configure) {
            filePath = configure.getRocketMqConsoleDataPath() + File.separator + FILE_NAME;
            if (!new File(filePath).exists()) {
                //Use the default path
                InputStream inputStream = getClass().getResourceAsStream("/" + FILE_NAME);
                if (inputStream == null) {
                    log.error(String.format("Can not found the file %s in Spring Boot jar", FILE_NAME));
                    System.out.printf(String.format("Can not found file %s in Spring Boot jar or %s, stop the  console starting",
                        FILE_NAME, configure.getRocketMqConsoleDataPath()));
                    System.exit(1);
                } else {
                    load(inputStream);
                }
            } else {
                log.info(String.format("Login Users configure file is %s", filePath));
                load();
                watch();
            }
        }

        private void load() {
            load(null);
        }

        private void load(InputStream inputStream) {

            Properties prop = new Properties();
            try {
                if (inputStream == null) {
                    prop.load(new FileReader(filePath));
                } else {
                    prop.load(inputStream);
                }
            } catch (Exception e) {
                log.error("load user.properties failed", e);
                throw new ServiceException(0, String.format("Failed to load loginUserInfo property file: %s", filePath));
            }

            Map<String, User> loadUserMap = new HashMap<>();
            String[] arrs;
            int role;
            for (String key : prop.stringPropertyNames()) {
                String v = prop.getProperty(key);
                if (v == null)
                    continue;
                arrs = v.split(",", 2);
                if (arrs.length == 0) {
                    continue;
                } else if (arrs.length == 1) {
                    role = 0;
                } else {
                    role = Integer.parseInt(arrs[1].trim());
                }

                loadUserMap.put(key, new User(key, arrs[0].trim(), role));
            }

            userMap.clear();
            userMap.putAll(loadUserMap);
        }

        private boolean watch() {
            try {
                FileWatchService fileWatchService = new FileWatchService(new String[] {filePath}, new FileWatchService.Listener() {
                    @Override
                    public void onChanged(String path) {
                        log.info("The loginUserInfo property file changed, reload the context");
                        load();
                    }
                });
                fileWatchService.start();
                log.info("Succeed to start LoginUserWatcherService");
                return true;
            } catch (Exception e) {
                log.error("Failed to start LoginUserWatcherService", e);
            }
            return false;
        }

        public User queryByName(String name) {
            return userMap.get(name);
        }

        public User queryByUsernameAndPassword(@NotNull String username, @NotNull String password) {
            User user = queryByName(username);
            if (user != null && password.equals(user.getPassword())) {
                return user.cloneOne();
            }

            return null;
        }
    }
}
