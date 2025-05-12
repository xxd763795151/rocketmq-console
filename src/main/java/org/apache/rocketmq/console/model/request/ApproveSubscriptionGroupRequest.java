package org.apache.rocketmq.console.model.request;

import lombok.Data;
import org.apache.rocketmq.console.model.dao.ApplySubscriptionGroupDO;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-19 19:29:01
 * @description rocketmq-console-ng
 **/
@Data
public class ApproveSubscriptionGroupRequest {

    private Long id;

    private String topic;

    private String subscriptionGroup;

    private Boolean consumeBroadcastEnable;

    private String subItem;

    private String approveRemark;

    private String username;

    private String accessKey;

    private String itemTeam;

    private Long itemTeamId;

    private Integer applyResult;

    private Integer applyStage;

    public ApplySubscriptionGroupDO convertTo(final ApplySubscriptionGroupDO groupDO) {
        ApplySubscriptionGroupDO result = groupDO == null ? new ApplySubscriptionGroupDO() : groupDO;
        result.setId(this.id);
        result.setTopic(this.topic);
        result.setSubscriptionGroup(this.subscriptionGroup);
        result.setConsumeBroadcastEnable(this.consumeBroadcastEnable ? 1 : 0);
        result.setSubItem(this.subItem);
        result.setApproveRemark(this.approveRemark);
        result.setUsername(this.username);
        result.setAccessKey(this.accessKey);
        result.setApplyStage(this.applyStage);
        result.setApplyResult(this.applyResult);

        return result;
    }

}
