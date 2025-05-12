package org.apache.rocketmq.console.model.dao;

import lombok.Data;
import org.apache.rocketmq.console.model.request.ApplySubscriptionGroupRequest;
import org.apache.rocketmq.console.util.EnvironmentUtil;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-19 16:58:52
 * @description rocketmq-console-ng
 **/
@Data
public class ApplySubscriptionGroupDO {

    private Long id;

    private String username;

    private String topic;

    private String subscriptionGroup;

    private String subItem;

    private String applyRemark;

    private String accessKey;

    private String secretKey;

    private String approveRemark;

    // apply stage, 0: wait approve, 1: complete
    private Integer applyStage;

    // apply result, 1: allow, 0: reject, 2: unknown
    private Integer applyResult;

    // notify status, 1: successful, 0: failed, 2: unknown
    private Integer sendStatus;

    private String createTime;

    // 1: true, 0: false
    private Integer consumeBroadcastEnable;

    private String env = EnvironmentUtil.getActiveEnvironment();

    public void merge(ApplySubscriptionGroupRequest request) {
        this.username = request.getUsername();
        this.subscriptionGroup = request.getSubscriptionGroup();
        this.subItem = request.getSubItem();
        this.applyRemark = request.getRemark();
        this.topic = request.getTopic();
        this.consumeBroadcastEnable = request.isConsumeBroadcastEnable() ? 1 : 0;
    }

}
