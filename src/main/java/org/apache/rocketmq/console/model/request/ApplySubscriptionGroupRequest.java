package org.apache.rocketmq.console.model.request;

import lombok.Data;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-14 19:59:10
 * @description rocketmq-console-ng
 **/
@Data
public class ApplySubscriptionGroupRequest {

    private String topic;

    private String subscriptionGroup;

    private String subItem;

    boolean consumeBroadcastEnable;

    private String remark;

    private String username;
}
