/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.console.service.impl;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.RollbackStats;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.console.aspect.admin.annotation.MultiMQAdminCmdMethod;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.dao.BelongItemMapper;
import org.apache.rocketmq.console.model.BelongItem;
import org.apache.rocketmq.console.model.ConsumerGroupRollBackStat;
import org.apache.rocketmq.console.model.GroupConsumeInfo;
import org.apache.rocketmq.console.model.GroupConsumeInfoExt;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.QueueStatInfo;
import org.apache.rocketmq.console.model.TopicConsumerInfo;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.DeleteSubGroupRequest;
import org.apache.rocketmq.console.model.request.ResetOffsetRequest;
import org.apache.rocketmq.console.service.AbstractCommonService;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.google.common.base.Throwables.propagate;

@Service
public class ConsumerServiceImpl extends AbstractCommonService implements ConsumerService {
    private Logger logger = LoggerFactory.getLogger(ConsumerServiceImpl.class);

    @Autowired
    private BelongItemMapper belongItemMapper;

    @Autowired
    private RMQConfigure configure;

    @Autowired
    private ExecutorService executorService;

    @Override
    @MultiMQAdminCmdMethod
    public List<GroupConsumeInfo> queryGroupList(User user) {
        Set<String> consumerGroupSet = Sets.newHashSet();
        try {
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            for (BrokerData brokerData : clusterInfo.getBrokerAddrTable().values()) {
                SubscriptionGroupWrapper subscriptionGroupWrapper = mqAdminExt.getAllSubscriptionGroup(brokerData.selectBrokerAddr(), 3000L);
                if (user.isAllData()) {
                    consumerGroupSet.addAll(subscriptionGroupWrapper.getSubscriptionGroupTable().keySet());
                } else {
                    Set<String> groupSet = belongItemMapper.selectSubscriptionByUser(user.getName()).stream().collect(Collectors.toSet());
                    List<String> groupList = subscriptionGroupWrapper.getSubscriptionGroupTable().keySet().stream().filter(x -> groupSet.contains(x)).collect(Collectors.toList());
                    consumerGroupSet.addAll(groupList);
                }
            }
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
        List<GroupConsumeInfo> groupConsumeInfoList = Lists.newArrayList();
        CompletionService<GroupConsumeInfo> completionService = new ExecutorCompletionService<>(executorService);
        for (String consumerGroup : consumerGroupSet) {
            groupConsumeInfoList.add(queryGroup(mqAdminExt, consumerGroup));
//            MQAdminExt ext = MQAdminInstance.threadLocalMQAdminExt();
            // todo: 多线程并发执行的方法不可取，因为admin实例只有一个，内部存在资源竞争，所以不能提升性能，所以需要构造多个admin实例来提升查询效率
//            completionService.submit(()->queryGroup(ext, consumerGroup));
        }
//        for (int i = 0; i < consumerGroupSet.size(); i++) {
//            try {
//                groupConsumeInfoList.add(completionService.take().get());
//            } catch (Exception e) {
//                logger.error("completionService.take() error", e);
//            }
//        }
        Collections.sort(groupConsumeInfoList);
        return groupConsumeInfoList;
    }

    @Override
    @MultiMQAdminCmdMethod
    public List<GroupConsumeInfoExt> queryGroupDetailList(User user) {
        List<GroupConsumeInfo> groupConsumeInfoList = queryGroupList(user);
        List<String> groupList = groupConsumeInfoList.stream().map(GroupConsumeInfo::getGroup).distinct().collect(Collectors.toList());
        List<BelongItem> belongItems = groupList.size() > 0 ? belongItemMapper.selectSubscriptionItem(groupList) : Collections.EMPTY_LIST;
        Map<String, List<Item>> map = new HashMap<>();
        belongItems.stream().forEach(belongItem -> {
            List<Item> list = map.getOrDefault(belongItem.getName(), new ArrayList<Item>());
            list.add(belongItem.getItem());
            map.put(belongItem.getName(), list);
        });
        List<GroupConsumeInfoExt> res = new ArrayList<>();
        groupConsumeInfoList.stream().forEach(groupConsumeInfo -> {
            GroupConsumeInfoExt consumeInfoExt = new GroupConsumeInfoExt();
            consumeInfoExt.setConsumeTps(groupConsumeInfo.getConsumeTps());
            consumeInfoExt.setConsumeType(groupConsumeInfo.getConsumeType());
            consumeInfoExt.setCount(groupConsumeInfo.getCount());
            consumeInfoExt.setDiffTotal(groupConsumeInfo.getDiffTotal());
            consumeInfoExt.setGroup(groupConsumeInfo.getGroup());
            consumeInfoExt.setMessageModel(groupConsumeInfo.getMessageModel());
            consumeInfoExt.setVersion(groupConsumeInfo.getVersion());
            consumeInfoExt.setItems(map.containsKey(groupConsumeInfo.getGroup()) ? map.get(groupConsumeInfo.getGroup()) : Collections.EMPTY_LIST);
            res.add(consumeInfoExt);
        });
        return res;
    }

    @Override
    @MultiMQAdminCmdMethod
    public GroupConsumeInfo queryGroup(String consumerGroup) {
        return queryGroup(mqAdminExt, consumerGroup);
    }

    @Override
    public List<TopicConsumerInfo> queryConsumeStatsListByGroupName(String groupName) {
        return queryConsumeStatsList(null, groupName);
    }

    @Override
    @MultiMQAdminCmdMethod
    public List<TopicConsumerInfo> queryConsumeStatsList(final String topic, String groupName) {
        ConsumeStats consumeStats = null;
        try {
            consumeStats = mqAdminExt.examineConsumeStats(groupName, topic);
        } catch (Exception e) {
            throw propagate(e);
        }
        List<MessageQueue> mqList = Lists.newArrayList(Iterables.filter(consumeStats.getOffsetTable().keySet(), new Predicate<MessageQueue>() {
            @Override
            public boolean apply(MessageQueue o) {
                return StringUtils.isBlank(topic) || o.getTopic().equals(topic);
            }
        }));
        Collections.sort(mqList);
        List<TopicConsumerInfo> topicConsumerInfoList = Lists.newArrayList();
        TopicConsumerInfo nowTopicConsumerInfo = null;
        Map<MessageQueue, String> messageQueueClientMap = getClientConnection(groupName);
        for (MessageQueue mq : mqList) {
            if (nowTopicConsumerInfo == null || (!StringUtils.equals(mq.getTopic(), nowTopicConsumerInfo.getTopic()))) {
                nowTopicConsumerInfo = new TopicConsumerInfo(mq.getTopic());
                topicConsumerInfoList.add(nowTopicConsumerInfo);
            }
            QueueStatInfo queueStatInfo = QueueStatInfo.fromOffsetTableEntry(mq, consumeStats.getOffsetTable().get(mq));
            queueStatInfo.setClientInfo(messageQueueClientMap.get(mq));
            nowTopicConsumerInfo.appendQueueStatInfo(queueStatInfo);
        }
        return topicConsumerInfoList;
    }

    private Map<MessageQueue, String> getClientConnection(String groupName) {
        Map<MessageQueue, String> results = Maps.newHashMap();
        try {
            ConsumerConnection consumerConnection = mqAdminExt.examineConsumerConnectionInfo(groupName);
            for (Connection connection : consumerConnection.getConnectionSet()) {
                String clinetId = connection.getClientId();
                ConsumerRunningInfo consumerRunningInfo = mqAdminExt.getConsumerRunningInfo(groupName, clinetId, false);
                for (MessageQueue messageQueue : consumerRunningInfo.getMqTable().keySet()) {
//                    results.put(messageQueue, clinetId + " " + connection.getClientAddr());
                    results.put(messageQueue, clinetId);
                }
            }
        } catch (Exception err) {
            logger.error("op=getClientConnection_error", err);
        }
        return results;
    }

    @Override
    @MultiMQAdminCmdMethod
    public Map<String /*groupName*/, TopicConsumerInfo> queryConsumeStatsListByTopicName(String topic) {
        Map<String, TopicConsumerInfo> group2ConsumerInfoMap = Maps.newHashMap();
        Set<String> existGroupList = existConsumerGroup();
        try {
            GroupList groupList = mqAdminExt.queryTopicConsumeByWho(topic);
            for (String group : groupList.getGroupList()) {
                if (CollectionUtils.isNotEmpty(existGroupList) && !existGroupList.contains(group)) {
                    continue;
                }
                List<TopicConsumerInfo> topicConsumerInfoList = null;
                try {
                    topicConsumerInfoList = queryConsumeStatsList(topic, group);
                } catch (Exception ignore) {
                }
                group2ConsumerInfoMap.put(group, CollectionUtils.isEmpty(topicConsumerInfoList) ? new TopicConsumerInfo(topic) : topicConsumerInfoList.get(0));
            }
            return group2ConsumerInfoMap;
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    @Override
    @MultiMQAdminCmdMethod
    public Map<String, ConsumerGroupRollBackStat> resetOffset(ResetOffsetRequest resetOffsetRequest) {
        Map<String, ConsumerGroupRollBackStat> groupRollbackStats = Maps.newHashMap();
        for (String consumerGroup : resetOffsetRequest.getConsumerGroupList()) {
            try {
                Map<MessageQueue, Long> rollbackStatsMap =
                    mqAdminExt.resetOffsetByTimestamp(resetOffsetRequest.getTopic(), consumerGroup, resetOffsetRequest.getResetTime(), resetOffsetRequest.isForce());
                ConsumerGroupRollBackStat consumerGroupRollBackStat = new ConsumerGroupRollBackStat(true);
                List<RollbackStats> rollbackStatsList = consumerGroupRollBackStat.getRollbackStatsList();
                for (Map.Entry<MessageQueue, Long> rollbackStatsEntty : rollbackStatsMap.entrySet()) {
                    RollbackStats rollbackStats = new RollbackStats();
                    rollbackStats.setRollbackOffset(rollbackStatsEntty.getValue());
                    rollbackStats.setQueueId(rollbackStatsEntty.getKey().getQueueId());
                    rollbackStats.setBrokerName(rollbackStatsEntty.getKey().getBrokerName());
                    rollbackStatsList.add(rollbackStats);
                }
                groupRollbackStats.put(consumerGroup, consumerGroupRollBackStat);
            } catch (MQClientException e) {
                if (ResponseCode.CONSUMER_NOT_ONLINE == e.getResponseCode()) {
                    try {
                        ConsumerGroupRollBackStat consumerGroupRollBackStat = new ConsumerGroupRollBackStat(true);
                        List<RollbackStats> rollbackStatsList = mqAdminExt.resetOffsetByTimestampOld(consumerGroup, resetOffsetRequest.getTopic(), resetOffsetRequest.getResetTime(), true);
                        consumerGroupRollBackStat.setRollbackStatsList(rollbackStatsList);
                        groupRollbackStats.put(consumerGroup, consumerGroupRollBackStat);
                        continue;
                    } catch (Exception err) {
                        logger.error("op=resetOffset_which_not_online_error", err);
                    }
                } else {
                    logger.error("op=resetOffset_error", e);
                }
                groupRollbackStats.put(consumerGroup, new ConsumerGroupRollBackStat(false, e.getMessage()));
            } catch (Exception e) {
                logger.error("op=resetOffset_error", e);
                groupRollbackStats.put(consumerGroup, new ConsumerGroupRollBackStat(false, e.getMessage()));
            }
        }
        return groupRollbackStats;
    }

    @Override
    @MultiMQAdminCmdMethod
    public List<ConsumerConfigInfo> examineSubscriptionGroupConfig(String group) {
        List<ConsumerConfigInfo> consumerConfigInfoList = Lists.newArrayList();
        try {
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            for (String brokerName : clusterInfo.getBrokerAddrTable().keySet()) { //foreach brokerName
                String brokerAddress = clusterInfo.getBrokerAddrTable().get(brokerName).selectBrokerAddr();
                SubscriptionGroupConfig subscriptionGroupConfig = mqAdminExt.examineSubscriptionGroupConfig(brokerAddress, group);
                if (subscriptionGroupConfig == null) {
                    continue;
                }
                consumerConfigInfoList.add(new ConsumerConfigInfo(Lists.newArrayList(brokerName), subscriptionGroupConfig));
            }
        } catch (Exception e) {
            throw propagate(e);
        }
        return consumerConfigInfoList;
    }

    @Override
    @MultiMQAdminCmdMethod
    public boolean deleteSubGroup(DeleteSubGroupRequest deleteSubGroupRequest) {
        try {
            String groupName = deleteSubGroupRequest.getGroupName();
            String retry = MixAll.RETRY_GROUP_TOPIC_PREFIX + groupName;
            String dlq = MixAll.DLQ_GROUP_TOPIC_PREFIX + groupName;
            // delete from db first, include retry and dlq info
            Map<String, Object> params = new HashMap<>();
            params.put("name", groupName);
            belongItemMapper.deleteBy(params);
            params.put("name", retry);
            belongItemMapper.deleteBy(params);
            params.put("name", dlq);
            belongItemMapper.deleteBy(params);
            // then delete from broker.The original logic is implemented.
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            for (String brokerName : deleteSubGroupRequest.getBrokerNameList()) {
                logger.info("addr={} groupName={}", clusterInfo.getBrokerAddrTable().get(brokerName).selectBrokerAddr(), deleteSubGroupRequest.getGroupName());
                mqAdminExt.deleteSubscriptionGroup(clusterInfo.getBrokerAddrTable().get(brokerName).selectBrokerAddr(), deleteSubGroupRequest.getGroupName());
            }
        } catch (Exception e) {
            throw propagate(e);
        }
        return true;
    }

    @Override
    public boolean createAndUpdateSubscriptionGroupConfig(ConsumerConfigInfo consumerConfigInfo) {
        try {
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            for (String brokerName : changeToBrokerNameSet(clusterInfo.getClusterAddrTable(),
                consumerConfigInfo.getClusterNameList(), consumerConfigInfo.getBrokerNameList())) {
                if (configure.getTraceNode().isSpecific() && brokerName.startsWith(configure.getTraceNode().getPrefix())) {
                    continue;
                }
                mqAdminExt.createAndUpdateSubscriptionGroupConfig(clusterInfo.getBrokerAddrTable().get(brokerName).selectBrokerAddr(), consumerConfigInfo.getSubscriptionGroupConfig());
            }
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
        return true;
    }

    @Override
    @MultiMQAdminCmdMethod
    public Set<String> fetchBrokerNameSetBySubscriptionGroup(String group) {
        Set<String> brokerNameSet = Sets.newHashSet();
        try {
            List<ConsumerConfigInfo> consumerConfigInfoList = examineSubscriptionGroupConfig(group);
            for (ConsumerConfigInfo consumerConfigInfo : consumerConfigInfoList) {
                brokerNameSet.addAll(consumerConfigInfo.getBrokerNameList());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return brokerNameSet;

    }

    @Override
    public ConsumerConnection getConsumerConnection(String consumerGroup) {
        try {
            return mqAdminExt.examineConsumerConnectionInfo(consumerGroup);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack) {
        try {
            return mqAdminExt.getConsumerRunningInfo(consumerGroup, clientId, jstack);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private Set<String> existConsumerGroup() {
        // 获取所有消费组名称
        Set<String> existGroupList = new HashSet<>();
        try {
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            Set<String> brokeAddrs = clusterInfo.getBrokerAddrTable().values().stream()
                .map(data -> data.getBrokerAddrs().get(MixAll.MASTER_ID)).collect(Collectors.toSet());
            brokeAddrs.forEach(addr -> {
                try {
                    SubscriptionGroupWrapper group = mqAdminExt.getAllSubscriptionGroup(addr, 2000L);
                    existGroupList.addAll(group.getSubscriptionGroupTable().keySet());
                } catch (Exception e) {
                    logger.error("getAllSubscriptionGroup failed, addr: " + addr, e);
                }
            });
        } catch (Exception e) {
            logger.error("获取消费组信息失败", e);
        }
        return existGroupList;
    }

    private GroupConsumeInfo queryGroup(MQAdminExt mqAdminExt, String consumerGroup) {
        GroupConsumeInfo groupConsumeInfo = new GroupConsumeInfo();
        try {
            ConsumeStats consumeStats = null;
            try {
                consumeStats = mqAdminExt.examineConsumeStats(consumerGroup);
            } catch (Exception e) {
                logger.warn("examineConsumeStats exception, " + consumerGroup, e);
            }

            ConsumerConnection consumerConnection = null;
            try {
                if (consumeStats != null) {
                    consumerConnection = mqAdminExt.examineConsumerConnectionInfo(consumerGroup);
                }
            } catch (Exception e) {
                logger.warn("examineConsumerConnectionInfo exception, " + consumerGroup, e);
            }

            groupConsumeInfo.setGroup(consumerGroup);

            if (consumeStats != null) {
                groupConsumeInfo.setConsumeTps((int) consumeStats.getConsumeTps());
                groupConsumeInfo.setDiffTotal(consumeStats.computeTotalDiff());
            }

            if (consumerConnection != null) {
                groupConsumeInfo.setCount(consumerConnection.getConnectionSet().size());
                groupConsumeInfo.setMessageModel(consumerConnection.getMessageModel());
                groupConsumeInfo.setConsumeType(consumerConnection.getConsumeType());
                groupConsumeInfo.setVersion(MQVersion.getVersionDesc(consumerConnection.computeMinVersion()));
            }
        } catch (Exception e) {
            logger.warn("examineConsumeStats or examineConsumerConnectionInfo exception, "
                + consumerGroup, e);
        }
        return groupConsumeInfo;
    }
}
