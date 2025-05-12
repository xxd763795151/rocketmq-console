package org.apache.rocketmq.console.model.request;

import lombok.Data;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-14 19:14:23
 * @description rocketmq-console-ng
 **/
@Data
public class ApplyTopicRequest {

    private String topic;

    private String producerGroup;

    private String subItem;

    private int queueNum = 8;

    private String remark;

    private String username;
}
