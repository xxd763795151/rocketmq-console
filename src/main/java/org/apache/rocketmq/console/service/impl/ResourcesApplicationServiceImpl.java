package org.apache.rocketmq.console.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.common.PlainAccessConfig;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.console.common.enumc.ApplyResult;
import org.apache.rocketmq.console.common.enumc.ApplyStage;
import org.apache.rocketmq.console.common.enumc.BelongType;
import org.apache.rocketmq.console.common.enumc.NotifyStatus;
import org.apache.rocketmq.console.config.EnvironmentConfig;
import org.apache.rocketmq.console.dao.ApplySubscriptionGroupMapper;
import org.apache.rocketmq.console.dao.ApplyTopicMapper;
import org.apache.rocketmq.console.dao.BelongItemMapper;
import org.apache.rocketmq.console.listener.event.ApproveNotifyEvent;
import org.apache.rocketmq.console.listener.event.ApproveSubscriptionGroupEvent;
import org.apache.rocketmq.console.listener.event.ApproveTopicEvent;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.dao.ApplySubscriptionGroupDO;
import org.apache.rocketmq.console.model.dao.ApplyTopicDO;
import org.apache.rocketmq.console.model.dao.BelongItemDO;
import org.apache.rocketmq.console.model.request.AclRequest;
import org.apache.rocketmq.console.model.request.ApplySubscriptionGroupRequest;
import org.apache.rocketmq.console.model.request.ApplyTopicRequest;
import org.apache.rocketmq.console.model.request.ApproveSubscriptionGroupRequest;
import org.apache.rocketmq.console.model.request.ApproveTopicRequest;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.DeleteSubGroupRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.model.vo.ApplySubscriptionGroupVO;
import org.apache.rocketmq.console.model.vo.ApplyTopicVO;
import org.apache.rocketmq.console.service.AbstractCommonService;
import org.apache.rocketmq.console.service.AclService;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.service.ResourcesApplicationService;
import org.apache.rocketmq.console.service.TopicService;
import org.apache.rocketmq.console.support.JsonResult;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-14 21:55:23
 * @description rocketmq-console-ng
 **/
@Slf4j
@Service
public class ResourcesApplicationServiceImpl extends AbstractCommonService implements ResourcesApplicationService {

    @Resource
    private MQAdminExt mqAdminExt;

    @Autowired
    private ApplyTopicMapper applyTopicMapper;

    @Autowired
    private AclService aclService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BelongItemMapper belongItemMapper;

    @Autowired
    private ApplySubscriptionGroupMapper applySubscriptionGroupMapper;

    @Autowired
    private EnvironmentConfig environmentConfig;

    @SneakyThrows @Override public Object applyTopic(ApplyTopicRequest request) {
        // topic存在不申请
        TopicList allTopics = mqAdminExt.fetchAllTopicList();
        Set<String> topicSet = allTopics.getTopicList();
        if (topicSet.contains(request.getTopic())) {
            return JsonResult.failed("topic已存在，不能重复创建");
        }

        // 正在申请该topic创建，不能重复申请
        ApplyTopicDO params = new ApplyTopicDO();
        params.setTopic(request.getTopic());
        params.setApplyStage(ApplyStage.WAIT_APPROVE.ordinal());
        List<ApplyTopicDO> applyTopicDOS = applyTopicMapper.selectApplyTopicDOList(params);
        if (CollectionUtils.isNotEmpty(applyTopicDOS)) {
            return JsonResult.failed("已申请创建该topic， 正在等待审批中");
        }

        // 提交创建请求
        ApplyTopicDO applyTopicDO = new ApplyTopicDO();
        applyTopicDO.merge(request);
        applyTopicDO.setApplyStage(ApplyStage.WAIT_APPROVE.ordinal());
        applyTopicDO.setApplyResult(ApplyResult.UNKNOWN.ordinal());
        applyTopicDO.setSendStatus(NotifyStatus.UNKNOWN.ordinal());
        for (String applyEnv : environmentConfig.getMultiApply()) {
            applyTopicDO.setEnv(applyEnv);
            applyTopicMapper.addApplyTopic(applyTopicDO);
        }

        applicationContext.publishEvent(new ApproveNotifyEvent(this, "topic", applyTopicDO, true));
        return JsonResult.success();
    }

    @SneakyThrows @Override public Object applySubscriptionGroup(ApplySubscriptionGroupRequest request) {

        // 已创建不能申请
        ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        Set<String> masterSet = new HashSet<>();
        Arrays.stream(clusterInfo.retrieveAllClusterNames()).forEach(clusterName -> {
            try {
                masterSet.addAll(CommandUtil.fetchMasterAddrByClusterName(mqAdminExt, clusterName));
            } catch (Exception e) {
                log.error("获取cluster name error.", e);
            }
        });

        if (masterSet.isEmpty()) {
            return JsonResult.failed("获取集群信息失败");
        }
        Set<String> groupSet = new HashSet<>();
        for (String addr : masterSet) {
            groupSet.addAll(mqAdminExt
                .getAllSubscriptionGroup(addr, 5000)
                .getSubscriptionGroupTable()
                .values()
                .stream()
                .map(SubscriptionGroupConfig::getGroupName)
                .collect(Collectors.toSet()));
        }

        if (groupSet.contains(request.getSubscriptionGroup())) {
            return JsonResult.failed("已创建该消费组");
        }
        // 正申请不能申请
        ApplySubscriptionGroupDO groupDO = new ApplySubscriptionGroupDO();
        groupDO.setSubscriptionGroup(request.getSubscriptionGroup());
        groupDO.setApplyStage(ApplyStage.WAIT_APPROVE.ordinal());
        List<ApplySubscriptionGroupDO> groupDOS = applySubscriptionGroupMapper.selectDOList(groupDO);
        if (CollectionUtils.isNotEmpty(groupDOS)) {
            return JsonResult.failed("已申请创建该消费组， 正在等待审批中");
        }
        // 申请
        groupDO.merge(request);
        groupDO.setApplyStage(ApplyStage.WAIT_APPROVE.ordinal());
        groupDO.setApplyResult(ApplyResult.UNKNOWN.ordinal());
        groupDO.setSendStatus(NotifyStatus.UNKNOWN.ordinal());

        for (String applyEnv : environmentConfig.getMultiApply()) {
            groupDO.setEnv(applyEnv);
            applySubscriptionGroupMapper.addApplySubscriptionGroup(groupDO);
        }

        applicationContext.publishEvent(new ApproveNotifyEvent(this, "消费组", groupDO, false));
        return JsonResult.success();
    }

    @Override public Object selectApplyTopicVOList(ApplyTopicRequest request, User user, int page, int limit) {
        PageHelper.startPage(page, limit);
        ApplyTopicDO applyTopicDO = new ApplyTopicDO();
        applyTopicDO.merge(request);
        if (!user.isAdmin()) {
            applyTopicDO.setUsername(user.getName());
        }
        List<ApplyTopicVO> applyTopicVOS = applyTopicMapper.selectApplyTopicVOList(applyTopicDO, user.getItem());
        return new PageInfo<>(applyTopicVOS);
    }

    @SneakyThrows @Override public Object approveTopic(ApproveTopicRequest request) {

        Optional<ApplyTopicDO> optionalApplyTopicDO = applyTopicMapper.selectById(request.getId());
        if (!optionalApplyTopicDO.isPresent()) {
            return JsonResult.failed("根据id找不到本次审批信息, id:  " + request.getId());
        }
        if (optionalApplyTopicDO.get().getApplyStage() == ApplyStage.COMPLETE.ordinal()) {
            return JsonResult.failed("已结束的审批不允许再发起");
        }

        // 已审批过不允许再审批
        // topic存在不申请
        TopicList allTopics = mqAdminExt.fetchAllTopicList();
        Set<String> topicSet = allTopics.getTopicList();
        if (topicSet.contains(request.getTopic())) {
            TopicRouteData topicRouteData = mqAdminExt.examineTopicRouteInfo(request.getTopic());
            Map<String, Integer> queueCount = new HashMap<>();
            topicRouteData.getQueueDatas().stream().forEach(qd -> queueCount.put(qd.getBrokerName(), qd.getWriteQueueNums()));
            Set<Integer> queueNum = new HashSet<>(queueCount.values());
            if (queueNum.size() > 1) {
                return JsonResult.failed("topic已存在，且已存在的topic路由信息中各个broker的队列数不相同，不能重复创建");
            } else if (queueNum.size() == 1) {
                Optional<Integer> first = queueNum.stream().findFirst();
                if (first.get() != request.getQueueNum()) {
                    return JsonResult.failed("topic已存在，请求创建的topic队列数与已存在topic列队数不同，操作无法继续");
                }
            }
        }

        boolean pass = request.getApplyResult() == ApplyResult.ALLOW.ordinal();

        if (pass) {
            // accessKey不存在，不创建
            Optional<PlainAccessConfig> plainAccessConfigOptional = aclService.getPlainAccessConfigByAccessKey(request.getAccessKey());
            if (!plainAccessConfigOptional.isPresent()) {
                return JsonResult.failed("accessKey不存在，不能创建, accessKey: " + request.getAccessKey());
            }

            // 创建 topic
            TopicConfigInfo topicConfigInfo = request.convertTo(TopicConfigInfo.class);
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            HashMap<String, Set<String>> clusterAddrTable = clusterInfo.getClusterAddrTable();
            Set<String> clusterName = clusterAddrTable.keySet();
            if (CollectionUtils.isEmpty(clusterName)) {
                return JsonResult.failed("mq集群名称获取失败");
            }
            topicConfigInfo.setClusterNameList(new ArrayList<>(clusterName));
            topicService.createOrUpdate(topicConfigInfo);

            // 更新 acl
            AclRequest aclRequest = new AclRequest();
            aclRequest.setConfig(plainAccessConfigOptional.get());
            aclRequest.setTopicPerm(concatTopicPerm(request.getTopic()));
            aclService.addOrUpdateAclTopicConfig(aclRequest);

            // 更新 belong item
            BelongItemDO belongItem = new BelongItemDO();
            belongItem.setItemId(request.getItemTeamId());
            belongItem.setName(request.getTopic());
            belongItem.setType(BelongType.TOPIC.ordinal());
            belongItemMapper.addIfNotExist(belongItem);

            // 更新申请信息
            ApplyTopicDO applyTopicDO = request.convertTo(new ApplyTopicDO());
            applyTopicDO.setAccessKey(plainAccessConfigOptional.get().getAccessKey());
            applyTopicDO.setSecretKey(plainAccessConfigOptional.get().getSecretKey());
            applyTopicMapper.updateApproveTopic(applyTopicDO);
        } else {
            applyTopicMapper.updateApplyReject(request.convertTo(new ApplyTopicDO()));
        }

        // 发送通知事件
        applicationContext.publishEvent(new ApproveTopicEvent(this, request.getId()));

        return JsonResult.success();
    }

    @Override public Object approveTopicResend(ApproveTopicRequest request) {
        // 发送通知事件
        applicationContext.publishEvent(new ApproveTopicEvent(this, request.getId()));

        return JsonResult.success();
    }

    @Override
    public Object selectApplySubscriptionGroupVOList(ApplySubscriptionGroupRequest request, User user, int page,
        int limit) {
        PageHelper.startPage(page, limit);
        ApplySubscriptionGroupDO groupDO = new ApplySubscriptionGroupDO();
        groupDO.merge(request);
        if (!user.isAdmin()) {
            groupDO.setUsername(user.getName());
        }
        List<ApplySubscriptionGroupVO> vos = applySubscriptionGroupMapper.selectVOList(groupDO, user.getItem());
        return new PageInfo<>(vos);
    }

    @SneakyThrows @Override public Object approveSubscriptionGroup(ApproveSubscriptionGroupRequest request) {

        // 已审批过的不允许再审批
        Optional<ApplySubscriptionGroupDO> groupDOOptional = applySubscriptionGroupMapper.selectById(request.getId());
        if (!groupDOOptional.isPresent()) {
            return JsonResult.failed("根据id找不到本次审批信息, id:  " + request.getId());
        }
        if (groupDOOptional.get().getApplyStage() == ApplyStage.COMPLETE.ordinal()) {
            return JsonResult.failed("已结束的审批不允许再发起");
        }

        // 审批通过还是拒绝
        boolean pass = request.getApplyResult() == ApplyResult.ALLOW.ordinal();

        if (pass) {
            // accessKey不存在不创建
            Optional<PlainAccessConfig> plainAccessConfigOptional = aclService.getPlainAccessConfigByAccessKey(request.getAccessKey());
            if (!plainAccessConfigOptional.isPresent()) {
                return JsonResult.failed("accessKey不存在，不能创建, accessKey: " + request.getAccessKey());
            }

            // 创建订阅组
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            Set<String> clusterNames = clusterInfo.getBrokerAddrTable().keySet();
            SubscriptionGroupConfig subscriptionGroupConfig = new SubscriptionGroupConfig();
            subscriptionGroupConfig.setGroupName(request.getSubscriptionGroup());
            subscriptionGroupConfig.setConsumeBroadcastEnable(request.getConsumeBroadcastEnable());
            ConsumerConfigInfo consumerConfigInfo = new ConsumerConfigInfo(new ArrayList<>(clusterNames), subscriptionGroupConfig);
            consumerService.createAndUpdateSubscriptionGroupConfig(consumerConfigInfo);

            // 更新 acl
            AclRequest aclRequest = new AclRequest();
            aclRequest.setConfig(plainAccessConfigOptional.get());
            aclRequest.setGroupPerm(concatGroupPerm(request.getSubscriptionGroup()));
            aclService.addOrUpdateAclGroupConfig(aclRequest);

            // 多个topic以逗号分隔
            Arrays.stream(request.getTopic().split(",")).map(t -> t.trim()).forEach(t -> {
                AclRequest ar = new AclRequest();
                ar.setConfig(plainAccessConfigOptional.get());
                ar.setTopicPerm(concatTopicPerm(t));
                aclService.addOrUpdateAclTopicConfig(ar);
            });

            // 更新belong
            BelongItemDO belongItem = new BelongItemDO();
            belongItem.setItemId(request.getItemTeamId());
            belongItem.setName(request.getSubscriptionGroup());
            belongItem.setType(BelongType.CONSUMER_GROUP.ordinal());
            belongItemMapper.addIfNotExist(belongItem);

            // 更新申请信息
            ApplySubscriptionGroupDO applySubscriptionGroupDO = request.convertTo(new ApplySubscriptionGroupDO());
            applySubscriptionGroupDO.setAccessKey(plainAccessConfigOptional.get().getAccessKey());
            applySubscriptionGroupDO.setSecretKey(plainAccessConfigOptional.get().getSecretKey());
            applySubscriptionGroupMapper.updateApproveSubscriptionGroup(applySubscriptionGroupDO);
        } else {
            applySubscriptionGroupMapper.updateApplyReject(request.convertTo(new ApplySubscriptionGroupDO()));
        }

        // 邮件通知
        applicationContext.publishEvent(new ApproveSubscriptionGroupEvent(this, request.getId()));
        return JsonResult.success();
    }

    @Override public Object approveSubscriptionGroupResend(ApproveSubscriptionGroupRequest request) {
        // 邮件通知
        applicationContext.publishEvent(new ApproveSubscriptionGroupEvent(this, request.getId()));
        return JsonResult.success();
    }

    @Override public Object revocation(long id, String type) {

        switch (type) {
            case "topic":
                revocationTopic(id);
                break;
            case "consumer":
                revocationConsumer(id);
                break;
            default:
                return JsonResult.failed("revocation type is invalid.");
        }
        return JsonResult.success();
    }

    @Scheduled(cron = "${cron.resources.apply.check}")
    public void notifyApprove() {
        ApplyTopicDO applyTopicDO = new ApplyTopicDO();
        applyTopicDO.setApplyStage(ApplyStage.WAIT_APPROVE.ordinal());
        List<ApplyTopicDO> applyTopicDOS = applyTopicMapper.selectApplyTopicDOList(applyTopicDO);
        if (CollectionUtils.isNotEmpty(applyTopicDOS)) {
            applicationContext.publishEvent(new ApproveNotifyEvent(this, "topic", applyTopicDOS, true));
            return;
        }

        ApplySubscriptionGroupDO applySubscriptionGroupDO = new ApplySubscriptionGroupDO();
        applySubscriptionGroupDO.setApplyStage(ApplyStage.WAIT_APPROVE.ordinal());
        List<ApplySubscriptionGroupDO> groupDOS = applySubscriptionGroupMapper.selectDOList(applySubscriptionGroupDO);
        if (CollectionUtils.isNotEmpty(groupDOS)) {
            applicationContext.publishEvent(new ApproveNotifyEvent(this, "消费组", groupDOS, false));
        }
    }

    private String concatTopicPerm(String topic) {
        return topic + "=PUB|SUB";
    }

    private String concatGroupPerm(String topic) {
        return topic + "=PUB|SUB";
    }

    private void revocationTopic(long id) {
        Optional<ApplyTopicDO> optional = applyTopicMapper.selectById(id);
        ApplyTopicDO aDo = optional.get();
        // apply stage, 0: wait approve, 1: complete
        aDo.setApplyStage(0);

        // notify status, 1: successful, 0: failed, 2: unknown
        aDo.setSendStatus(1);

        aDo.setApproveRemark("");
        // apply result, 1: allow, 0: reject, 2: unknown
        if (aDo.getApplyResult() == 1) {
            // 删除创建的topic
            topicService.deleteTopic(aDo.getTopic());
            // 移除配置的access Key
            AclRequest request = new AclRequest();
            Optional<PlainAccessConfig> plainAccessConfigOptional = aclService.getPlainAccessConfigByAccessKey(aDo.getAccessKey());
            request.setConfig(plainAccessConfigOptional.get());
            request.setTopicPerm(aDo.getTopic());
            aclService.deletePermConfig(request);

            // 更新信息
            aDo.setAccessKey("");
            aDo.setSecretKey("");
        }
        aDo.setApplyResult(2);
        applyTopicMapper.updateApproveTopic(aDo);
    }

    private void revocationConsumer(long id) {
        Optional<ApplySubscriptionGroupDO> optional = applySubscriptionGroupMapper.selectById(id);
        ApplySubscriptionGroupDO aDo = optional.get();
        // apply stage, 0: wait approve, 1: complete
        aDo.setApplyStage(0);

        // notify status, 1: successful, 0: failed, 2: unknown
        aDo.setSendStatus(1);

        aDo.setApproveRemark("");
        // apply result, 1: allow, 0: reject, 2: unknown
        if (aDo.getApplyResult() == 1) {
            // 删除创建的consumer group
            DeleteSubGroupRequest deleteSubGroupRequest = new DeleteSubGroupRequest();
            deleteSubGroupRequest.setGroupName(aDo.getSubscriptionGroup());
            try {
                deleteSubGroupRequest.setBrokerNameList(new ArrayList<>(mqAdminExt.examineBrokerClusterInfo().getBrokerAddrTable().keySet()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            consumerService.deleteSubGroup(deleteSubGroupRequest);
            // 移除配置的access Key
            AclRequest request = new AclRequest();
            Optional<PlainAccessConfig> plainAccessConfigOptional = aclService.getPlainAccessConfigByAccessKey(aDo.getAccessKey());
            request.setConfig(plainAccessConfigOptional.get());
            // 不删除topic权限，容易出问题，因为可能已经配置的有topic权限了
//            request.setTopicPerm(aDo.getTopic());
            request.setGroupPerm(aDo.getSubscriptionGroup());
            aclService.deletePermConfig(request);

            // 更新信息
            aDo.setAccessKey("");
            aDo.setSecretKey("");
        }
        aDo.setApplyResult(2);
        applySubscriptionGroupMapper.updateApproveSubscriptionGroup(aDo);
    }
}
