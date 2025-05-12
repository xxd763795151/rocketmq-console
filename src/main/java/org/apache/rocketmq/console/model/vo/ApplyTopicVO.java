package org.apache.rocketmq.console.model.vo;

import lombok.Data;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 14:17:41
 * @description rocketmq-console-ng
 **/
@Data
public class ApplyTopicVO {

    private Long id;

    private String username;

    private String topic;

    private String producerGroup;

    private String subItem;

    private Integer queueNum;

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

    private String itemTeam;

    private Long itemTeamId;
}
