package org.apache.rocketmq.console.service;

import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.ApplySubscriptionGroupRequest;
import org.apache.rocketmq.console.model.request.ApplyTopicRequest;
import org.apache.rocketmq.console.model.request.ApproveSubscriptionGroupRequest;
import org.apache.rocketmq.console.model.request.ApproveTopicRequest;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-14 21:54:22
 * @description rocketmq-console-ng
 **/
public interface ResourcesApplicationService {

    Object applyTopic(ApplyTopicRequest request);

    Object applySubscriptionGroup(ApplySubscriptionGroupRequest request);

    Object selectApplyTopicVOList(ApplyTopicRequest request, User user, int page, int limit);

    Object approveTopic(ApproveTopicRequest request);

    Object approveTopicResend(ApproveTopicRequest request);

    Object selectApplySubscriptionGroupVOList(ApplySubscriptionGroupRequest request, User user, int page, int limit);

    Object approveSubscriptionGroup(ApproveSubscriptionGroupRequest request);

    Object approveSubscriptionGroupResend(ApproveSubscriptionGroupRequest request);

    Object revocation(long id, String type);
}
