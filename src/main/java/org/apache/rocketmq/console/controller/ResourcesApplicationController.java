package org.apache.rocketmq.console.controller;

import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.console.interceptor.security.NotAdminDisable;
import org.apache.rocketmq.console.model.request.ApplySubscriptionGroupRequest;
import org.apache.rocketmq.console.model.request.ApplyTopicRequest;
import org.apache.rocketmq.console.model.request.ApproveSubscriptionGroupRequest;
import org.apache.rocketmq.console.model.request.ApproveTopicRequest;
import org.apache.rocketmq.console.service.ResourcesApplicationService;
import org.apache.rocketmq.console.support.JsonResult;
import org.apache.rocketmq.console.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-14 19:19:10
 * @description rocketmq-console-ng
 **/
@RestController
@RequestMapping("/apply")
@Slf4j
public class ResourcesApplicationController {

    @Autowired
    private ResourcesApplicationService resourcesApplicationService;

    @PostMapping("/topic.do")
    public Object applyTopic(@RequestBody ApplyTopicRequest request) {

        Objects.requireNonNull(request.getTopic(), "topic is null");
        Objects.requireNonNull(request.getProducerGroup(), "getProducerGroup is null");
        Objects.requireNonNull(request.getSubItem(), "getSubItem is null");
        Objects.requireNonNull(request.getUsername(), "getUsername is null");
        log.info("applyTopic: {}", request.toString());
        return resourcesApplicationService.applyTopic(request);
    }

    @PostMapping("/subscription.do")
    public Object applySubscriptionGroup(@RequestBody ApplySubscriptionGroupRequest request) {

        Objects.requireNonNull(request.getTopic(), "topic is null");
        Objects.requireNonNull(request.getSubscriptionGroup(), "getSubscriptionGroup is null");
        Objects.requireNonNull(request.getSubItem(), "getSubItem is null");
        Objects.requireNonNull(request.getUsername(), "getUsername is null");
        log.info("applySubscriptionGroup: {}", request.toString());
        return resourcesApplicationService.applySubscriptionGroup(request);
    }

    @GetMapping("/topic/list")
    public Object applyTopicList(ApplyTopicRequest request, int page, int limit) {
        return resourcesApplicationService.selectApplyTopicVOList(request, WebUtil.getLoginInfo().getUser(), page, limit);
    }

    @NotAdminDisable
    @PostMapping("/topic/approve.do")
    public Object approveTopic(@RequestBody ApproveTopicRequest request) {
        Objects.requireNonNull(request.getTopic(), "topic is null");
        Objects.requireNonNull(request.getProducerGroup(), "getProducerGroup is null");
        Objects.requireNonNull(request.getSubItem(), "getSubItem is null");
        Objects.requireNonNull(request.getUsername(), "getUsername is null");
        Objects.requireNonNull(request.getAccessKey(), "getAccessKey is null");
        return resourcesApplicationService.approveTopic(request);
    }

    @NotAdminDisable
    @PostMapping("/topic/approve/resend.do")
    public Object approveTopicResend(@RequestBody ApproveTopicRequest request) {
        Objects.requireNonNull(request.getId(), "is is null");
        return resourcesApplicationService.approveTopicResend(request);
    }

    @NotAdminDisable
    @PostMapping("/subscription/approve/resend.do")
    public Object approveSubscriptionResend(@RequestBody ApproveSubscriptionGroupRequest request) {
        Objects.requireNonNull(request.getId(), "is is null");
        return resourcesApplicationService.approveSubscriptionGroupResend(request);
    }

    @GetMapping("/subscription/list")
    public Object applySubscriptionList(ApplySubscriptionGroupRequest request, int page, int limit) {
        return resourcesApplicationService.selectApplySubscriptionGroupVOList(request, WebUtil.getLoginInfo().getUser(), page, limit);
    }

    @NotAdminDisable
    @PostMapping("/subscription/approve.do")
    public Object approveTopic(@RequestBody ApproveSubscriptionGroupRequest request) {
        Objects.requireNonNull(request.getTopic(), "topic is null");
        Objects.requireNonNull(request.getSubscriptionGroup(), "getSubscriptionGroup is null");
        Objects.requireNonNull(request.getSubItem(), "getSubItem is null");
        Objects.requireNonNull(request.getUsername(), "getUsername is null");
        Objects.requireNonNull(request.getAccessKey(), "getAccessKey is null");
        return resourcesApplicationService.approveSubscriptionGroup(request);
    }

    @NotAdminDisable
    @PostMapping("/revocation.do")
    public Object revocation(@RequestBody Map<String, Object> params) {
        String type = (String) params.get("type");
        long id = Long.valueOf((String) params.get("id"));
        if (!("topic".equals(type) || "consumer".equals(type))) {
            return JsonResult.failed("revocation type is invalid.");
        }
        return resourcesApplicationService.revocation(id, type);
    }

}
