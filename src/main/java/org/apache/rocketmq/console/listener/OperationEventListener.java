package org.apache.rocketmq.console.listener;

import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.rocketmq.console.common.SendMailClient;
import org.apache.rocketmq.console.config.EmailConfig;
import org.apache.rocketmq.console.config.EnvironmentConfig;
import org.apache.rocketmq.console.listener.event.ForgetPasswordEvent;
import org.apache.rocketmq.console.listener.event.RegisterAccountEvent;
import org.apache.rocketmq.console.model.AccountOperationRecord;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.UpdateUserRequest;
import org.apache.rocketmq.console.service.AccountOperationRecordService;
import org.apache.rocketmq.console.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-10 17:36:36
 * @description rocketmq-console-ng
 **/
@Component
public class OperationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationEventListener.class);

    @Autowired
    private SendMailClient mailClient;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Autowired
    private AccountOperationRecordService accountOperationRecordService;

    @EventListener
    public void registerAccount(RegisterAccountEvent event) {
        LOGGER.info("register account event: {}", event.getUser());

        UpdateUserRequest user = event.getUser();
        Map<String, Object> body = new HashMap<>();
        body.put("username", user.getName());
        body.put("password", user.getPassword());
        body.put("environment", environmentConfig.getAccount());
        boolean sendSuccess = false;
        try {
            sendSuccess = mailClient.sendHtml(emailConfig.getUsername(), user.getEmail(), "RocketMQ控制台登录账户注册成功", "register-account", body);
        } catch (Exception e) {
            LOGGER.error("发送通知邮件异常", e);
        }

        try {
            Optional<User> optionalUser = userService.selectUserByID(user.getId());
            if (!optionalUser.isPresent()) {
                throw new IllegalArgumentException("根据id: " + user.getId() + "， 找不到用户信息");
            }
            User newUser = optionalUser.get();
            // 记录操作日志
            AccountOperationRecord record = new AccountOperationRecord();
            record.setUsername(newUser.getName());
            record.setPassword(newUser.getPassword());
            record.setEmail(newUser.getEmail());
            record.setItemId(newUser.getItem().getId());
            record.setItemName(newUser.getItem().getName());
            record.setRoleId(newUser.getRole().getId());
            record.setRoleName(newUser.getRole().getName());
            record.setOperationType(AccountOperationType.REGISTER.ordinal());
            record.setSendMessage(JSONObject.toJSONString(body, false));
            record.setSendStatus(sendSuccess ? 1 : 0);

            accountOperationRecordService.add(record);
        } catch (Exception e) {
            LOGGER.error("记录注册账户信息异常", e);
        }

    }

    @EventListener
    public void forgetPassword(ForgetPasswordEvent event) {
        LOGGER.info("forget password event: {}", event.getUser());

        User user = event.getUser();
        Map<String, Object> body = new HashMap<>();
        body.put("username", user.getName());
        body.put("password", user.getPassword());
        body.put("environment", environmentConfig.getAccount());
        boolean sendSuccess = false;
        try {
            sendSuccess = mailClient.sendHtml(emailConfig.getUsername(), user.getEmail(), "RocketMQ控制台密码找回", "forget-password", body);
        } catch (Exception e) {
            LOGGER.error("发送通知邮件异常", e);
        }

        try {
            Optional<User> optionalUser = userService.selectUserByID(user.getId());
            if (!optionalUser.isPresent()) {
                throw new IllegalArgumentException("根据id: " + user.getId() + "， 找不到用户信息");
            }
            User newUser = optionalUser.get();
            // 记录操作日志
            AccountOperationRecord record = new AccountOperationRecord();
            record.setUsername(newUser.getName());
            record.setPassword(newUser.getPassword());
            record.setEmail(newUser.getEmail());
            record.setItemId(newUser.getItem().getId());
            record.setItemName(newUser.getItem().getName());
            record.setRoleId(newUser.getRole().getId());
            record.setRoleName(newUser.getRole().getName());
            record.setOperationType(AccountOperationType.FORGET_PASSWORD.ordinal());
            record.setSendMessage(JSONObject.toJSONString(body, false));
            record.setSendStatus(sendSuccess ? 1 : 0);

            accountOperationRecordService.add(record);
        } catch (Exception e) {
            LOGGER.error("记录找回密码账户信息异常", e);
        }

    }
}
