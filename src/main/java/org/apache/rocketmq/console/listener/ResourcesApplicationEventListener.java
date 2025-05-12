package org.apache.rocketmq.console.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanMap;
import org.apache.rocketmq.console.common.SendMailClient;
import org.apache.rocketmq.console.common.enumc.ApplyResult;
import org.apache.rocketmq.console.common.enumc.NotifyStatus;
import org.apache.rocketmq.console.config.EmailConfig;
import org.apache.rocketmq.console.config.EnvironmentConfig;
import org.apache.rocketmq.console.dao.ApplySubscriptionGroupMapper;
import org.apache.rocketmq.console.dao.ApplyTopicMapper;
import org.apache.rocketmq.console.dao.UserMapper;
import org.apache.rocketmq.console.listener.event.ApproveNotifyEvent;
import org.apache.rocketmq.console.listener.event.ApproveSubscriptionGroupEvent;
import org.apache.rocketmq.console.listener.event.ApproveTopicEvent;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.dao.ApplySubscriptionGroupDO;
import org.apache.rocketmq.console.model.dao.ApplyTopicDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-19 11:35:38
 * @description rocketmq-console-ng
 **/
@Component
@Slf4j
public class ResourcesApplicationEventListener {

    @Autowired
    private SendMailClient mailClient;

    @Autowired
    private ApplyTopicMapper applyTopicMapper;

    @Autowired
    private ApplySubscriptionGroupMapper applySubscriptionGroupMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Value("${approve.notify.email}")
    private String notifyEmail;

    @EventListener
    public void approveTopic(ApproveTopicEvent event) {
        log.info("approve topic event: {}", event);

        Optional<ApplyTopicDO> applyTopicDOOptional = applyTopicMapper.selectById(event.getApplyId());
        ApplyTopicDO applyTopicDO = applyTopicDOOptional.get();
        Optional<User> optionalUser = userMapper.selectUserByName(applyTopicDO.getUsername());
        if (!optionalUser.isPresent()) {
            log.error("user not exist, username: {}", applyTopicDO.getUsername());
            return;
        }

        User user = optionalUser.get();
        Map<String, Object> body = new HashMap<>();
        body.put("accessKey", applyTopicDO.getAccessKey());
        body.put("secretKey", applyTopicDO.getSecretKey());
        body.put("remark", applyTopicDO.getApproveRemark());
        body.put("env", environmentConfig.getResources());
        body.put("topic", applyTopicDO.getTopic());
        body.put("pass", applyTopicDO.getApplyResult() == ApplyResult.ALLOW.ordinal());

        boolean sendSuccess = false;
        try {
            sendSuccess = mailClient.sendHtml(emailConfig.getUsername(), user.getEmail(),
                "RocketMQ Topic申请流程结束，环境【" + environmentConfig.getResources() + "】", "apply-topic", body);
        } catch (Exception e) {
            log.error("发送通知邮件异常", e);
        }

        ApplyTopicDO params = new ApplyTopicDO();
        params.setId(applyTopicDO.getId());
        params.setSendStatus(sendSuccess ? NotifyStatus.SUCCESSFUL.ordinal() : NotifyStatus.FAILED.ordinal());
        applyTopicMapper.updateSendStatus(params);
    }

    @EventListener
    public void approveSubscriptionGroup(ApproveSubscriptionGroupEvent event) {
        log.info("approve subscription group event: {}", event);

        Optional<ApplySubscriptionGroupDO> groupDOOptional = applySubscriptionGroupMapper.selectById(event.getApplyId());
        ApplySubscriptionGroupDO applySubscriptionGroupDO = groupDOOptional.get();
        Optional<User> optionalUser = userMapper.selectUserByName(applySubscriptionGroupDO.getUsername());
        if (!optionalUser.isPresent()) {
            log.error("user not exist, username: {}", applySubscriptionGroupDO.getUsername());
            return;
        }

        User user = optionalUser.get();
        Map<String, Object> body = new HashMap<>();
        body.put("accessKey", applySubscriptionGroupDO.getAccessKey());
        body.put("secretKey", applySubscriptionGroupDO.getSecretKey());
        body.put("remark", applySubscriptionGroupDO.getApproveRemark());
        body.put("env", environmentConfig.getResources());
        body.put("subscriptionGroup", applySubscriptionGroupDO.getSubscriptionGroup());
        body.put("pass", applySubscriptionGroupDO.getApplyResult() == ApplyResult.ALLOW.ordinal());

        boolean sendSuccess = false;
        try {
            sendSuccess = mailClient.sendHtml(emailConfig.getUsername(), user.getEmail(),
                "RocketMQ 消费组申请流程结束，环境【" + environmentConfig.getResources() + "】", "apply-subscription-group", body);
        } catch (Exception e) {
            log.error("发送通知邮件异常", e);
        }

        ApplySubscriptionGroupDO params = new ApplySubscriptionGroupDO();
        params.setId(applySubscriptionGroupDO.getId());
        params.setSendStatus(sendSuccess ? NotifyStatus.SUCCESSFUL.ordinal() : NotifyStatus.FAILED.ordinal());
        applySubscriptionGroupMapper.updateSendStatus(params);
    }

    @EventListener
    public void approveNotify(ApproveNotifyEvent event) {
        log.info("approve notify event: {}", event);
        Map<String, Object> body = new HashMap<>();
        body.put("env", environmentConfig.getResources());
        body.put("type", event.getType());
        List<Map<Object, Object>> list = new ArrayList<>();

        Map<String, User> userMap = new HashMap<>();
        String itemName = "项目组";
        if (event.getData() instanceof List) {
            List<Object> objects = (List<Object>) event.getData();
            objects.stream().forEach(o -> {
                Map<Object, Object> map = new HashMap<>(new BeanMap(o));
                if (o instanceof ApplySubscriptionGroupDO) {
                    ApplySubscriptionGroupDO groupDO = (ApplySubscriptionGroupDO) o;
                    map.remove("consumeBroadcastEnable");
                    map.put("consumeBroadcastEnable", groupDO.getConsumeBroadcastEnable() == 1);
                }

                String username = (String) map.get("username");
                if (!userMap.containsKey(username)) {
                    userMap.put(username, userMapper.selectUserByName(username).get());
                }
                map.put("itemName", userMap.get(username).getItem().getName());
                map.put("email", userMap.get(username).getEmail());

                list.add(map);
            });
        } else {
            Object o = event.getData();
            Map<Object, Object> map = new HashMap<>(new BeanMap(o));
            if (o instanceof ApplySubscriptionGroupDO) {
                ApplySubscriptionGroupDO groupDO = (ApplySubscriptionGroupDO) o;
                map.remove("consumeBroadcastEnable");
                map.put("consumeBroadcastEnable", groupDO.getConsumeBroadcastEnable() == 1);
            }
            String username = (String) map.get("username");
            if (!userMap.containsKey(username)) {
                userMap.put(username, userMapper.selectUserByName(username).get());
            }
            itemName = userMap.get(username).getItem().getName();
            map.put("itemName", itemName);
            map.put("email", userMap.get(username).getEmail());

            list.add(map);
        }
        body.put("list", list);
        body.put("isTopic", event.isTopic());
        for (String to : notifyEmail.split(",")) {
            try {
                mailClient.sendHtml(emailConfig.getUsername(), to,
                    itemName + "申请" + event.getType(), "approve-notify", body);
            } catch (Exception e) {
                log.error("发送通知邮件异常", e);
            }
        }
    }
}
