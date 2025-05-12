package org.apache.rocketmq.console.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.console.common.ExpireCleanCache;
import org.apache.rocketmq.console.common.SendMailClient;
import org.apache.rocketmq.console.config.EmailConfig;
import org.apache.rocketmq.console.config.RegisterConfig;
import org.apache.rocketmq.console.dao.AccountOperationRecordMapper;
import org.apache.rocketmq.console.listener.AccountOperationType;
import org.apache.rocketmq.console.model.AccountOperationRecord;
import org.apache.rocketmq.console.service.AccountOperationRecordService;
import org.apache.rocketmq.console.support.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-13 15:52:34
 * @description rocketmq-console-ng
 **/
@Service
public class AccountOperationRecordServiceImpl implements AccountOperationRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountOperationRecordServiceImpl.class);

    @Autowired
    private AccountOperationRecordMapper accountOperationRecordMapper;

    @Autowired
    private SendMailClient mailClient;

    @Autowired
    private EmailConfig emailConfig;

    private RegisterConfig registerConfig;

    private ExpireCleanCache<String, Object> expireCleanCache;

    private AccountOperationRecordServiceImpl(@Autowired RegisterConfig registerConfig) {
        this.registerConfig = registerConfig;
        this.expireCleanCache = new ExpireCleanCache<>(registerConfig.getEmailInterval());
    }

    @Override public int add(AccountOperationRecord record) {
        return accountOperationRecordMapper.add(record);
    }

    @Override public PageInfo select(AccountOperationRecord record, int page, int limit) {
        PageHelper.startPage(page, limit);
        List<AccountOperationRecord> records = accountOperationRecordMapper.select(record);
        return new PageInfo(records);
    }

    @Override public Object resend(AccountOperationRecord record) {
        if (expireCleanCache.contains(record.getEmail())) {
            return new JsonResult(-9999, "操作过于频繁，请间隔" + registerConfig.getEmailInterval() / 1000 + "秒");
        } else {
            expireCleanCache.put(record.getEmail(), record.getEmail());
        }
        Map<String, Object> body = JSONObject.parseObject(record.getSendMessage());
        StringBuilder errorMsg = new StringBuilder();
        String subject = record.getOperationType() == AccountOperationType.REGISTER.ordinal() ? "RocketMQ控制台用户登录信息" : "RocketMQ控制台找回密码";
        String template = record.getOperationType() == AccountOperationType.REGISTER.ordinal() ? "register-account" : "forget-password";
        boolean sendSuccess = mailClient.sendHtml(emailConfig.getUsername(), record.getEmail(), subject, template, body, errorMsg);
        if (sendSuccess) {
            try {
                AccountOperationRecord params = new AccountOperationRecord();
                params.setId(record.getId());
                params.setSendStatus(1);
                accountOperationRecordMapper.update(params);
                return new JsonResult(0, "邮件发送成功");
            } catch (Exception e) {
                LOGGER.error("更新发送状态失败", e);
                return new JsonResult(-9999, "邮件发送成功，但是更新发送状态失败");
            }
        }
        // 清除缓存，发送失败了，允许前端再多试几次
        expireCleanCache.remove(record.getEmail());
        return new JsonResult(-9999, "邮件发送失败，请检查邮箱是否正确或其它原因， 错误信息: " + errorMsg.toString());
    }
}
