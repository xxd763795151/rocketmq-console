package org.apache.rocketmq.console.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.BrokerStatsItem;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.apache.rocketmq.console.common.SendMessageAdapter;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.BelongItem;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.service.BelongItemService;
import org.apache.rocketmq.console.service.UserService;
import org.apache.rocketmq.console.support.CommonExecutors;
import org.apache.rocketmq.console.util.LogUtil;
import org.apache.rocketmq.srvutil.FileWatchService;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import static org.apache.rocketmq.store.stats.BrokerStatsManager.GROUP_GET_NUMS;
import static org.apache.rocketmq.store.stats.BrokerStatsManager.GROUP_GET_SIZE;
import static org.apache.rocketmq.store.stats.BrokerStatsManager.SNDBCK_PUT_NUMS;
import static org.apache.rocketmq.store.stats.BrokerStatsManager.TOPIC_PUT_NUMS;
import static org.apache.rocketmq.store.stats.BrokerStatsManager.TOPIC_PUT_SIZE;

/**
 * @Author xuxd
 * @Date 11:32 上午 2020/11/17
 * @Description NotifyTask   定时采集信息，发送指定收件人
 **/
@Component
public class NotifyTask implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyTask.class);

    private static final String NOTIFY_CONFIG_NAME = "notify.yml";
    private static final String NOTIFY_DATA_NAME = "notify_data.json";

    interface Common {
        String BROKER = "broker";
    }

    interface Send {
        String NUMS = "sendNums";
        String TPS = "sendTps";
        String SIZE = "sendSize";
        String SPEED = "sendSpeed";
        String TOPIC = "topic";
        int COLUMN_SIZE = 6;
        String TOPIC_LIST = "topics";
    }

    interface Consumer {
        String GROUP = "consumer";
        String TOPIC = "topic";
        String CONSUME_NUMS = "consumeNums";
        String CONSUME_TPS = "consumeTps";
        String CONSUME_SIZE = "consumeSize";
        String CONSUME_SPEED = "consumeSpeed";
        String SEND_BACK_NUMS = "sendBackNums";
        int COLUMN_SIZE = 8;
        String CONSUMER_LIST = "consumers";
    }

    private static List<NotifyConfig> notifyConfigs = new ArrayList<>();

    @Autowired
    private MQAdminExt mqAdminExt;

    @Autowired
    private RMQConfigure rmqConfigure;

    @Autowired
    private SendMessageAdapter sendMessageAdapter;

    @Autowired
    private UserService userService;

    @Autowired
    private BelongItemService belongItemService;

    @Scheduled(cron = "${task.collect.time}")
    public void collectData() {
        try {
            LOGGER.info("Start collect data.");
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            HashMap<String, BrokerData> brokerDataHashMap = clusterInfo.getBrokerAddrTable();

            CompletionService completionService = new ExecutorCompletionService(CommonExecutors.EXECUTOR_SERVICE);
            Map<String, Object> collectData = Maps.newHashMap();

            brokerDataHashMap.values().stream().forEach(bd -> {
                String brokerName = bd.getBrokerName();
                String addr = bd.selectBrokerAddr();

                completionService.submit((Callable<Map<String, Object>>) () -> {
                    LOGGER.info("Collect broker[{}] addr[{}] data", brokerName, addr);
                    Map<String, Object> result = Maps.newHashMap();
                    result.put(brokerName, collectBrokerData(brokerName, addr));
                    return result;
                });
            });

            for (int i = 0; i < brokerDataHashMap.size(); i++) {
                try {
                    Map<String, Object> map = (Map<String, Object>) (completionService.poll(1, TimeUnit.MINUTES).get());
                    collectData.putAll(map != null ? map : Maps.newHashMap());
                } catch (InterruptedException ignore) {

                }
            }

            // flush data to disk
            saveAsJson(collectData);
            LOGGER.info("End collect data.");
        } catch (Exception e) {
            LOGGER.error("examineBrokerClusterInfo error", e);
        }
    }

    private void saveAsJson(Object data) throws IOException {
        String jsonString = JSON.toJSONString(data, true);
        String filePath = rmqConfigure.getRocketMqConsoleDataPath() + File.separator + NOTIFY_DATA_NAME;
        MixAll.string2File(jsonString, filePath);
    }

    @Scheduled(cron = "${task.notify.time}")
    public void notifyResponsible() {
        // deserialize notification data
        String filePath = rmqConfigure.getRocketMqConsoleDataPath() + File.separator + NOTIFY_DATA_NAME;
        File file = new File(filePath);
        if (!file.exists()) {
            LOGGER.warn("File {} not exist", filePath);
            return;
        }
        try {

            List<User> notifyUserList = userService.selectNotifyUser();
            // why do not query with a sql only once, but query more than once;
            List<Item> items = notifyUserList.stream().map(user -> user.getItem()).collect(Collectors.toList());
            List<BelongItem> belongItems = belongItemService.selectByItemList(items);

            String jsonString = MixAll.file2String(filePath);
            JSONObject jsonObject = JSON.parseObject(jsonString);
            List<NotifyConfig> notifyConfigList = toNotifyConfig(notifyUserList, belongItems);
            notifyConfigList.stream().forEach(config -> {
                Map<String, Object> content = filterData(jsonObject, config);
                List<String> topicList = (List<String>) content.get(Send.TOPIC_LIST);
                List<String> consumerList = (List<String>) content.get(Consumer.CONSUMER_LIST);
                if (CollectionUtils.isNotEmpty(topicList) || CollectionUtils.isNotEmpty(consumerList)) {
                    sendMessageAdapter.send(config.getTarget(), content, config.getType());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Read notification error, path: " + filePath, e);
        }

    }

    private List<NotifyConfig> toNotifyConfig(List<User> userList, List<BelongItem> items) {
        Map<Long, List<String>> topicMap = Maps.newHashMap();
        Map<Long, List<String>> consumerMap = Maps.newHashMap();

        items.stream().forEach(bi -> {
            // type: 1 topic, 2 group
            if (bi.getType() == 1) {
                List<String> topicList = topicMap.getOrDefault(bi.getItem().getId(), new ArrayList<>());
                topicList.add(bi.getName());
                topicMap.put(bi.getItem().getId(), topicList);
            } else {
                List<String> consumerList = consumerMap.getOrDefault(bi.getItem().getId(), new ArrayList<>());
                consumerList.add(bi.getName());
                consumerMap.put(bi.getItem().getId(), consumerList);
            }
        });
        List<NotifyConfig> result = new ArrayList<>();
        userList.stream().filter(user -> user.getNotify() != 0 && StringUtils.isNotBlank(user.getEmail())).forEach(user -> {
            NotifyConfig config = new NotifyConfig();
            config.setTarget(user.getEmail());
            config.setTopics(topicMap.containsKey(user.getItem().getId()) ? topicMap.get(user.getItem().getId()) : Collections.EMPTY_LIST);
            config.setConsumers(consumerMap.containsKey(user.getItem().getId()) ? consumerMap.get(user.getItem().getId()) : Collections.EMPTY_LIST);
            config.setRole(user.getRole().getType());
            result.add(config);
        });
        return result;
    }

    private Map<String, Object> filterData(JSONObject json, NotifyConfig config) {
        List<String> topics = config.getTopics();
        List<String> consumers = config.getConsumers();
        Map<String, Object> result = Maps.newHashMap();
        Set<String> topicSet = new HashSet<>(topics);
        Set<String> consumerSet = new HashSet<>(consumers);
        List<Map<String, Object>> topicList = new ArrayList<>();
        List<Map<String, Object>> groupList = new ArrayList<>();
        json.keySet().stream().forEach(
            broker -> {
                JSONObject brokerJson = json.getJSONObject(broker);
                JSONObject topicJson = brokerJson.getJSONObject("topic");

                topicJson.values().stream()
                    .filter(
                        o -> {
                            JSONObject jsonObject = (JSONObject) o;
                            return jsonObject.containsKey(Send.TOPIC) && (config.getRole() == 1 || topicSet.contains(jsonObject.getString(Send.TOPIC)));
                        }
                    )
                    .forEach(
                        o -> {
                            Map<String, Object> map = Maps.newHashMap((JSONObject) o);
                            map.put(Common.BROKER, broker);
                            if (map.size() >= Send.COLUMN_SIZE) {
                                topicList.add(map);
                            }
                        });

                JSONObject consumerJson = brokerJson.getJSONObject("consumer");
                consumerJson.values().stream()
                    .filter(
                        o -> {
                            JSONObject jsonObject = (JSONObject) o;
                            return jsonObject.containsKey(Consumer.TOPIC)
                                && jsonObject.containsKey(Consumer.GROUP)
                                && (config.getRole() == 1 || consumerSet.contains(jsonObject.getString(Consumer.GROUP)));
                        })
                    .forEach(
                        o -> {
                            Map<String, Object> map = Maps.newHashMap((JSONObject) o);
                            map.put(Common.BROKER, broker);
                            if (map.size() >= Consumer.COLUMN_SIZE) {
                                groupList.add(map);
                            }
                        });
            });
        topicList.sort((x, y) -> {
            String topic1 = (String) x.get(Send.TOPIC);
            String topic2 = (String) y.get(Send.TOPIC);
            if (!topic1.equals(topic2)) {
                return topic1.compareTo(topic2);
            } else {
                return ((String) x.get(Common.BROKER)).compareTo((String) y.get(Common.BROKER));
            }
        });
        result.put(Send.TOPIC_LIST, topicList);

        groupList.sort((x, y) -> {
            String group1 = (String) x.get(Consumer.GROUP);
            String group2 = (String) y.get(Consumer.GROUP);
            if (!group1.equals(group2)) {
                return group1.compareTo(group2);
            }
            String topic1 = (String) x.get(Consumer.TOPIC);
            String topic2 = (String) y.get(Consumer.TOPIC);
            if (!topic1.equals(topic2)) {
                return topic1.compareTo(topic2);
            } else {
                return ((String) x.get(Common.BROKER)).compareTo((String) y.get(Common.BROKER));
            }
        });
        result.put(Consumer.CONSUMER_LIST, groupList);

        return result;
    }

    @Override
    public void afterSingletonsInstantiated() {
        NotifyConfigWatcher watcher = new NotifyConfigWatcher(rmqConfigure);
        watcher.start();
        LOGGER.info("Start monitoring configuration file modification");
    }

    private Map<String, Object> collectBrokerData(String brokerName, String brokerAddr) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("topic", collectTopicMetrics(brokerName, brokerAddr));
        result.put("consumer", collectConsumerMetrics(brokerName, brokerAddr));
        return result;
    }

    private Map<String, Object> collectTopicMetrics(String brokerName, String brokerAddr) {
        Map<String, Object> result = Maps.newHashMap();
        try {
            mqAdminExt.fetchAllTopicList().getTopicList().stream().filter(topic -> !TopicValidator.isSystemTopic(topic)).forEach(topic -> {
                try {
                    Map<String, Object> map = Maps.newHashMap();
                    result.put(topic, map);
                    BrokerStatsData statsData = null;
                    try {
                        statsData = mqAdminExt.viewBrokerStatsData(brokerAddr, TOPIC_PUT_NUMS, topic);
                        BrokerStatsItem statsDay = statsData.getStatsDay();
                        map.put(Send.TOPIC, topic);
                        map.put(Send.NUMS, statsDay.getSum());
                        map.put(Send.TPS, keep1Precision(statsDay.getTps()));
                    } catch (Exception e) {
                        LogUtil.statsErrorLog(LOGGER, TOPIC_PUT_NUMS + " failed", e);
                    }
                    try {
                        statsData = mqAdminExt.viewBrokerStatsData(brokerAddr, TOPIC_PUT_SIZE, topic);
                        BrokerStatsItem statsDay = statsData.getStatsDay();
                        map.put(Send.SIZE, convertByteUnit(statsDay.getSum(), 'm'));
                        map.put(Send.SPEED, convertByteUnit(statsDay.getTps(), 'k'));
                    } catch (Exception e) {
                        LogUtil.statsErrorLog(LOGGER, TOPIC_PUT_SIZE + " failed", e);
                    }
                } catch (Exception e) {
                    LogUtil.statsErrorLog(LOGGER, "viewBrokerStatsData failed", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("collectTopicMetrics error", e);
        }
        return result;
    }

    private Map<String, Object> collectConsumerMetrics(String brokerName, String brokerAddr) {
        Map<String, Object> result = Maps.newHashMap();
        try {
            SubscriptionGroupWrapper subscriptionGroup = mqAdminExt.getAllSubscriptionGroup(brokerAddr, 3000);
            subscriptionGroup.getSubscriptionGroupTable().keySet().stream().forEach(group -> {
                ConsumerConnection consumerConnection = null;
                try {
                    consumerConnection = mqAdminExt.examineConsumerConnectionInfo(group);
                } catch (Exception e) {
                    LOGGER.error("examineConsumerConnectionInfo error", e);
                }
                if (consumerConnection != null) {
                    consumerConnection.getSubscriptionTable().values().forEach(subData -> {
                        String topic = subData.getTopic();
                        try {
                            String key = topic + "@" + group;
                            BrokerStatsData statsData = mqAdminExt.viewBrokerStatsData(brokerAddr, GROUP_GET_NUMS, key);
                            BrokerStatsItem statsDay = statsData.getStatsDay();
                            Map<String, Object> map = Maps.newHashMap();
                            result.put(key, map);
                            map.put(Consumer.TOPIC, topic);
                            map.put(Consumer.GROUP, group);
                            map.put(Common.BROKER, brokerName);
                            map.put(Consumer.CONSUME_NUMS, statsDay.getSum());
                            map.put(Consumer.CONSUME_TPS, keep1Precision(statsDay.getTps()));
                            statsData = mqAdminExt.viewBrokerStatsData(brokerAddr, GROUP_GET_SIZE, key);
                            statsDay = statsData.getStatsDay();
                            map.put(Consumer.CONSUME_SIZE, convertByteUnit(statsDay.getSum(), 'm'));
                            map.put(Consumer.CONSUME_SPEED, convertByteUnit(statsDay.getTps(), 'k'));

                            try {
                                statsData = mqAdminExt.viewBrokerStatsData(brokerAddr, SNDBCK_PUT_NUMS, key);
                                statsDay = statsData.getStatsDay();
                                map.put(Consumer.SEND_BACK_NUMS, statsDay.getSum());
                            } catch (Exception e) {
                                map.put(Consumer.SEND_BACK_NUMS, 0);
                            }
                        } catch (Exception e) {
                            LogUtil.statsErrorLog(LOGGER, "viewBrokerStatsData failed", e);
                        }

                    });
                }
            });

        } catch (Exception e) {
            LOGGER.error("collectConsumerMetrics error", e);
        }
        return result;
    }

    private String keep1Precision(double d) {
        return String.format("%.1f", d);
    }

    private String convertByteUnit(double num, char u) {
        double kib = 1024;
        double mib = 1024 * kib;
        double gib = 1024 * mib;
        switch (u) {
            case 'g':
                return String.format("%.2f", num / gib);
            case 'm':
                return String.format("%.2f", num / mib);
            case 'k':
                return String.format("%.2f", num / kib);
            case 'b':
                return String.format("%.0f", num);
            default:
                break;
        }
        if (num > gib) {
            return String.format("%.2f Gib", num / gib);
        }
        if (num > mib) {
            return String.format("%.2f Mib", num / mib);
        }
        if (num > kib) {
            return String.format("%.2f kb", num / kib);
        }
        return String.format("%.0f byte", num);
    }

    static class NotifyConfigWatcher {
        private RMQConfigure configure;
        private String filePath;

        public NotifyConfigWatcher(RMQConfigure configure) {
            this.configure = configure;
            this.filePath = configure.getRocketMqConsoleDataPath() + File.separator + NOTIFY_CONFIG_NAME;
        }

        public void start() {
            load();
            watch();
        }

        private void load() {
            try {
                File configFile = new File(filePath);
                try (InputStream inputStream = configFile.exists() ? new FileInputStream(configFile) : getClass().getResourceAsStream("/" + NOTIFY_CONFIG_NAME)) {
                    notifyConfigs.clear();
                    notifyConfigs.addAll(toNotifyConfigs(inputStream));
                }
            } catch (Exception e) {
                LOGGER.error("load notify.yml error", e);
            }
            LOGGER.info("notify config: {}", notifyConfigs);
        }

        private void watch() {
            try {
                FileWatchService service = new FileWatchService(new String[] {filePath}, path -> {
                    LOGGER.info("File modify, path: {}", path);
                    load();
                });
                service.start();
                LOGGER.info("Start File watch, file path: {}", filePath);
            } catch (Exception e) {
                LOGGER.error("Start file watch error", e);
            }
        }

        private List<NotifyConfig> toNotifyConfigs(InputStream inputStream) {
            Yaml yaml = new Yaml();
            JSONObject jsonObject = yaml.loadAs(inputStream, JSONObject.class);
            List<NotifyConfig> configs = new ArrayList<>();
            if (jsonObject.containsKey("notify")) {
                JSONArray array = jsonObject.getJSONArray("notify");
                if (array != null) {
                    for (int i = 0; i < array.size(); i++) {
                        configs.add(array.getObject(i, NotifyConfig.class));
                    }
                }
            }
            return configs;
        }
    }

    static class NotifyConfig {
        private String type = "email";
        private List<String> topics;
        private List<String> consumers;
        private String target;
        private int role;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }

        public List<String> getConsumers() {
            return consumers;
        }

        public void setConsumers(List<String> consumers) {
            this.consumers = consumers;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public int getRole() {
            return role;
        }

        public void setRole(int role) {
            this.role = role;
        }
    }
}
