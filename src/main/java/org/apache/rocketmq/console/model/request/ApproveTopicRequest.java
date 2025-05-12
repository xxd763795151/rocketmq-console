package org.apache.rocketmq.console.model.request;

import lombok.Data;
import org.apache.rocketmq.console.App;
import org.apache.rocketmq.console.model.dao.ApplyTopicDO;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-16 16:19:11
 * @description rocketmq-console-ng
 **/
@Data
public class ApproveTopicRequest {

    private Long id;

    private String topic;

    private String producerGroup;

    private String subItem;

    private int queueNum = 8;

    private String approveRemark = "";

    private String username;

    private String accessKey;

    private String itemTeam;

    private Long itemTeamId;

    private Integer applyResult;

    private Integer applyStage;

    public TopicConfigInfo convertTo(Class<? extends TopicConfigInfo> cls) {
        TopicConfigInfo topicConfigInfo = new TopicConfigInfo();
        topicConfigInfo.setReadQueueNums(this.queueNum);
        topicConfigInfo.setWriteQueueNums(this.queueNum);
        topicConfigInfo.setPerm(6);
        topicConfigInfo.setTopicName(this.topic);
        topicConfigInfo.setOrder(false);
        return topicConfigInfo;
    }

    public ApplyTopicDO convertTo(final ApplyTopicDO applyTopicDO) {
        ApplyTopicDO result = applyTopicDO == null ? new ApplyTopicDO() : applyTopicDO;
        result.setId(this.id);
        result.setTopic(this.topic);
        result.setProducerGroup(this.producerGroup);
        result.setSubItem(this.subItem);
        result.setQueueNum(this.queueNum);
        result.setApproveRemark(this.approveRemark);
        result.setUsername(this.username);
        result.setAccessKey(this.accessKey);
        result.setApplyResult(this.applyResult);
        result.setApplyStage(this.applyStage);
        return result;
    }
}
