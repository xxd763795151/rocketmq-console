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

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.dao.BelongItemMapper;
import org.apache.rocketmq.console.dao.UserMapper;
import org.apache.rocketmq.console.model.BelongItem;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.SendTopicMessageRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.AbstractCommonService;
import org.apache.rocketmq.console.service.TopicService;
import org.apache.rocketmq.console.service.client.AclClientHookHolder;
import org.apache.rocketmq.console.support.CommonExecutors;
import org.apache.rocketmq.tools.command.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TopicServiceImpl extends AbstractCommonService implements TopicService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicServiceImpl.class);

    @Autowired
    private RMQConfigure rMQConfigure;

    @Autowired
    private BelongItemMapper belongItemMapper;

    @Autowired
    private UserMapper userMapper;

    private AtomicLong instanceNums = new AtomicLong(0);

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public TopicList fetchAllTopicList(boolean skipSysProcess, User user) {
        try {
            TopicList allTopics = mqAdminExt.fetchAllTopicList();
//            if (skipSysProcess) {
//                return allTopics;
//            }

            Set<String> topics = new HashSet<>();
            if (!user.isAllData()) {
                List<String> userTopicList = belongItemMapper.selectTopicByUser(user.getName());
                Set<String> userTopicSet = new HashSet<>();
                if (userTopicList != null) {
                    userTopicSet.addAll(userTopicList);
                }
                topics.addAll(allTopics.getTopicList().stream().filter(topic -> userTopicSet.contains(topic)).collect(Collectors.toSet()));
            } else {
                TopicList sysTopics = getSystemTopicList();
                for (String topic : allTopics.getTopicList()) {
                    if (sysTopics.getTopicList().contains(topic)) {
                        topics.add(String.format("%s%s", "%SYS%", topic));
                    } else {
                        topics.add(topic);
                    }
                }
            }
            allTopics.getTopicList().clear();
            allTopics.getTopicList().addAll(topics);
            return allTopics;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public TopicStatsTable stats(String topic) {
        try {
            return mqAdminExt.examineTopicStats(topic);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public TopicRouteData route(String topic) {
        try {
            return mqAdminExt.examineTopicRouteInfo(topic);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public GroupList queryTopicConsumerInfo(String topic) {
        try {
            return mqAdminExt.queryTopicConsumeByWho(topic);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void createOrUpdate(TopicConfigInfo topicCreateOrUpdateRequest) {
        TopicConfig topicConfig = new TopicConfig();
        BeanUtils.copyProperties(topicCreateOrUpdateRequest, topicConfig);
        try {
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            for (String brokerName : changeToBrokerNameSet(clusterInfo.getClusterAddrTable(),
                topicCreateOrUpdateRequest.getClusterNameList(), topicCreateOrUpdateRequest.getBrokerNameList())) {
                if (rMQConfigure.getTraceNode().isSpecific() && brokerName.startsWith(rMQConfigure.getTraceNode().getPrefix())) {
                    continue;
                }
                mqAdminExt.createAndUpdateTopicConfig(clusterInfo.getBrokerAddrTable().get(brokerName).selectBrokerAddr(), topicConfig);
            }
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    @Override
    public TopicConfig examineTopicConfig(String topic, String brokerName) {
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return mqAdminExt.examineTopicConfig(clusterInfo.getBrokerAddrTable().get(brokerName).selectBrokerAddr(), topic);
    }

    @Override
    public List<TopicConfigInfo> examineTopicConfig(String topic) {
        List<TopicConfigInfo> topicConfigInfoList = Lists.newArrayList();
        TopicRouteData topicRouteData = route(topic);
        for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
            TopicConfigInfo topicConfigInfo = new TopicConfigInfo();
            TopicConfig topicConfig = examineTopicConfig(topic, brokerData.getBrokerName());
            BeanUtils.copyProperties(topicConfig, topicConfigInfo);
            topicConfigInfo.setBrokerNameList(Lists.newArrayList(brokerData.getBrokerName()));
            topicConfigInfoList.add(topicConfigInfo);
        }
        return topicConfigInfoList;
    }

    @Override
    public boolean deleteTopic(String topic, String clusterName) {
        try {
            if (StringUtils.isBlank(clusterName)) {
                return deleteTopic(topic);
            }
            Set<String> masterSet = CommandUtil.fetchMasterAddrByClusterName(mqAdminExt, clusterName);
            mqAdminExt.deleteTopicInBroker(masterSet, topic);
            Set<String> nameServerSet = null;
            if (StringUtils.isNotBlank(rMQConfigure.getNamesrvAddr())) {
                String[] ns = rMQConfigure.getNamesrvAddr().split(";");
                nameServerSet = new HashSet<String>(Arrays.asList(ns));
            }
            mqAdminExt.deleteTopicInNameServer(nameServerSet, topic);
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
        return true;
    }

    @Override
    public boolean deleteTopic(String topic) {
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
        // delete from db
        Map<String, Object> params = new HashMap<>();
        params.put("name", topic);
        belongItemMapper.deleteBy(params);
        for (String clusterName : clusterInfo.getClusterAddrTable().keySet()) {
            deleteTopic(topic, clusterName);
        }
        return true;
    }

    @Override
    public boolean deleteTopicInBroker(String brokerName, String topic) {

        try {
            ClusterInfo clusterInfo = null;
            try {
                clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            mqAdminExt.deleteTopicInBroker(Sets.newHashSet(clusterInfo.getBrokerAddrTable().get(brokerName).selectBrokerAddr()), topic);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return true;
    }

    private TopicList getSystemTopicList() {
        DefaultMQProducer producer = new DefaultMQProducer(MixAll.SELF_TEST_PRODUCER_GROUP, AclClientHookHolder.HOLDER.getAclHook());
        producer.setInstanceName(String.valueOf(System.currentTimeMillis()));
        producer.setNamesrvAddr(rMQConfigure.getNamesrvAddr());

        try {
            producer.start();
            return producer.getDefaultMQProducerImpl().getmQClientFactory().getMQClientAPIImpl().getSystemTopicList(20000L);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            producer.shutdown();
        }
    }

    @Override
    public SendResult sendTopicMessageRequest(SendTopicMessageRequest sendTopicMessageRequest) {
        DefaultMQProducer producer = new DefaultMQProducer(MixAll.SELF_TEST_PRODUCER_GROUP, AclClientHookHolder.HOLDER.getAclHook());
        producer.setInstanceName(String.valueOf(System.currentTimeMillis()));
        producer.setNamesrvAddr(rMQConfigure.getNamesrvAddr());
        try {
            producer.start();
            Message msg = new Message(sendTopicMessageRequest.getTopic(),
                sendTopicMessageRequest.getTag(),
                sendTopicMessageRequest.getKey(),
                sendTopicMessageRequest.getMessageBody().getBytes()
            );
            return producer.send(msg);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            producer.shutdown();
        }
    }

    @Override
    public Map<String, Object> selectTopicListWithItem(boolean skipSysProcess, User user) {
        TopicList topicList = fetchAllTopicList(skipSysProcess, user);
        Set<String> topics = topicList.getTopicList();
        String brokerAddr = topicList.getBrokerAddr();
        List<BelongItem> belongItems = topics != null && topics.size() > 0 ? belongItemMapper.selectTopicItem(new ArrayList<>(topics)) : Collections.EMPTY_LIST;
        Map<String, Object> res = new HashMap<>();
        Map<String, List<Item>> topicMap = new HashMap<>();
        belongItems.stream().forEach(item -> {
            List<Item> list = topicMap.getOrDefault(item.getName(), new ArrayList<>());
            list.add(item.getItem());
            topicMap.put(item.getName(), list);
            topics.remove(item.getName());
        });
        topics.stream().forEach(topic -> topicMap.put(topic, new ArrayList<>()));
        res.put("brokerAddr", brokerAddr);
        res.put("topicList", topicMap.keySet().stream().map(key -> {
            Map<String, Object> map = new HashMap<>();
            map.put("topic", key);
            map.put("item", topicMap.get(key));
            return map;
        })
            .collect(Collectors.toList()));
        return res;
    }

    @Override public Map<String, Object> sendStats(String topic) {
        Map<String, Object> res = new HashMap<>();
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(MixAll.TOOLS_CONSUMER_GROUP, AclClientHookHolder.HOLDER.getAclHook());
        try {
            consumer.setInstanceName(getInstanceName());
            consumer.start();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long today = calendar.getTimeInMillis();
            AtomicLong msgNums = new AtomicLong(0L);

            Map<MessageQueue, Long> todayStartOffsetTable = offsetForTimestamp(consumer, topic, today, true);
//            Map<MessageQueue, Long> todayEndOffsetTable = offsetForTimestamp(consumer, topic, System.currentTimeMillis(), false);
//            todayStartOffsetTable
//                .entrySet()
//                .stream()
//                .forEach(entry -> {
//                    MessageQueue mq = entry.getKey();
//                    if (todayEndOffsetTable.containsKey(mq)) {
//                        Long maxOffset = todayEndOffsetTable.get(mq);
//                        if (maxOffset > entry.getValue()) {
//                            msgNums.addAndGet(maxOffset - entry.getValue());
//                        }
//                    }
//                });
            TopicStatsTable topicStatsTable = mqAdminExt.examineTopicStats(topic);
            HashMap<MessageQueue, TopicOffset> offsetTable = topicStatsTable.getOffsetTable();
            if (todayStartOffsetTable.size() != todayStartOffsetTable.size()) {
                throw new IllegalArgumentException("queue info not match");
            }
            offsetTable.entrySet().stream().forEach(entry -> {
                MessageQueue mq = entry.getKey();
                // 这个判断条件很奇怪，是因为rocketmq broker根据时间戳获取偏移有个不太预期的bug，有兴趣看源码。这些判断条件是为处理各种场景下的问题我才想出来的，必须判断时间戳
                if (entry.getValue().getLastUpdateTimestamp() >= today && todayStartOffsetTable.containsKey(mq)) {
                    Long maxOffset = entry.getValue().getMaxOffset();
                    if (maxOffset > todayStartOffsetTable.get(mq)) {
                        msgNums.addAndGet(maxOffset - todayStartOffsetTable.get(mq));
                    }
                }
            });
            res.put("today", msgNums.get());

            Calendar yesterdayStart = calendar;
            yesterdayStart.add(Calendar.DAY_OF_MONTH, -1);
            long yesterdayStartTime = yesterdayStart.getTimeInMillis();
            Calendar yesterdayEnd = yesterdayStart;
            yesterdayEnd.add(Calendar.DAY_OF_MONTH, 1);
            yesterdayEnd.add(Calendar.MILLISECOND, -1);
            long yesterdayEndTime = yesterdayEnd.getTimeInMillis();
            Map<MessageQueue, Long> yesStartOffsetTable = offsetForTimestamp(consumer, topic, yesterdayStartTime, true);
            Map<MessageQueue, Long> yesEndOffsetTable = offsetForTimestamp(consumer, topic, yesterdayEndTime, false);

            if (yesStartOffsetTable.size() != yesEndOffsetTable.size()) {
                throw new IllegalArgumentException("queue info not match");
            }
            AtomicLong yesterdayNums = new AtomicLong(0L);
            yesStartOffsetTable.entrySet().stream().forEach(entry -> {
                MessageQueue mq = entry.getKey();
                if (yesEndOffsetTable.containsKey(mq)) {
                    Long maxOffset = yesEndOffsetTable.get(mq);
                    if (maxOffset > entry.getValue()) {
                        yesterdayNums.addAndGet(maxOffset - entry.getValue());
                    }
                }
            });
            res.put("yesterday", yesterdayNums.get());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            consumer.shutdown();
        }
        return res;
    }

    private String getInstanceName() {
        return "rocketmq-console@" + Thread.currentThread().getId() + "#" + instanceNums.getAndIncrement();
    }

    private Map<MessageQueue, Long> offsetForTimestamp(final DefaultMQPullConsumer pullConsumer,
        final String topic,
        final long time, boolean isGreater) throws MQClientException, ExecutionException, InterruptedException {
        Map<MessageQueue, Long> result = new HashMap<>();
        Collection<MessageQueue> messageQueues = pullConsumer.fetchSubscribeMessageQueues(topic);
        CompletionService<Map<MessageQueue, Long>> completionService = new ExecutorCompletionService<>(CommonExecutors.EXECUTOR_SERVICE);

        messageQueues.stream().forEach(mq -> {
            completionService.submit(() -> {
                Map<MessageQueue, Long> res = new HashMap<>();
                try {
                    long offset = pullConsumer.searchOffset(mq, time);
                    if (offset == 0) { //perhaps not found or offset from 0
                        long minOffset = pullConsumer.minOffset(mq);
                        if (minOffset > 0) {
                            long maxOffset = pullConsumer.maxOffset(mq);
                            if (maxOffset != 0) {
                                res.put(mq, maxOffset);
                                return res;
                            }
                        }
                    }

                    PullResult pullResult = pullConsumer.pull(mq, "*", offset, 2);
                    if (pullResult.getPullStatus() == PullStatus.FOUND) {
                        List<MessageExt> foundList = pullResult.getMsgFoundList();
                        MessageExt targetMessage = null;
                        for (MessageExt msg : foundList) {
                            if (targetMessage == null) {
                                targetMessage = msg;
                            } else {
                                if (isGreater) {
                                    if (targetMessage.getStoreTimestamp() < time) {
                                        if (msg.getStoreTimestamp() > targetMessage.getStoreTimestamp()) {
                                            targetMessage = msg;
                                        }
                                    }
                                } else {
                                    if (targetMessage.getStoreTimestamp() > time) {
                                        if (msg.getStoreTimestamp() < targetMessage.getStoreTimestamp()) {
                                            targetMessage = msg;
                                        }
                                    }
                                }
                            }
                        }
                        res.put(mq, targetMessage.getQueueOffset());
                    } else {
                        res.put(mq, offset);
                    }
                } catch (MQClientException e) {
                    LOGGER.error("offsetForTimestamp error", e);
                }
                return res;
            });
        });

        for (int i = 0; i < messageQueues.size(); i++) {
            result.putAll(completionService.take().get());
        }

        return result;
    }
}
