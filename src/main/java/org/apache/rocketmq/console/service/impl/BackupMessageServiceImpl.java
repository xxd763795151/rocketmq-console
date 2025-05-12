package org.apache.rocketmq.console.service.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.console.model.ResponseData;
import org.apache.rocketmq.console.service.BackupMessageService;
import org.apache.rocketmq.console.util.HttpClientUtil;
import org.apache.rocketmq.remoting.common.RemotingUtil;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.springframework.stereotype.Service;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-07-05 15:09:44
 **/
@Slf4j
@Service
public class BackupMessageServiceImpl implements BackupMessageService {

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final long scanInterval = 50 * 1000;

    private final long expireTime = 20 * 1000;

    private final ThreadSafeTreeMap<Long, ReputNode> reputCache = new ThreadSafeTreeMap<>(new Comparator<Long>() {
        @Override public int compare(Long o1, Long o2) {
            return o1.compareTo(o2);
        }
    });

    private final AtomicLong instanceNum = new AtomicLong(0);

    @Resource
    private MQAdminExt mqAdminExt;

    @PostConstruct
    public void init() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            Set<Long> keySet = reputCache.keySet();
            keySet.stream().forEach(k -> {
                ReputNode node = reputCache.get(k);
                if (node != null && node.getLastUpdate() + expireTime < System.currentTimeMillis()) {
                    log.warn("remove expire reput: {}", node.url);
                    reputCache.remove(k);
                }
            });
        }, scanInterval, scanInterval, TimeUnit.MILLISECONDS);
    }

    @Override public Map<String, Object> registerReput(Map<String, String> params) {
        log.info("rocketmq reput register: {}", params.toString());
        Map<String, Object> res = new HashMap<>();

        try {
            String timeRange = params.get("timeRange");
            String[] timeArr = timeRange.split("-");
            int startH = Integer.valueOf(timeArr[0]);// 前多少时
            int endH = Integer.valueOf(timeArr[1]);
            long startTime = getMillByHour(startH);
            long endTime = getMillByHour(endH);
            String url = params.get("url");
            if (!reputCache.containsKey(startTime)) {
                reputCache.put(startTime, new ReputNode(startTime, endTime, url, System.currentTimeMillis()));
            } else {
                ReputNode node = reputCache.get(startTime);
                if (!node.getUrl().equals(url) || node.getEndTime() != endTime) {
                    reputCache.put(startTime, new ReputNode(startTime, endTime, url, System.currentTimeMillis()));
                }
            }
            reputCache.get(startTime).setLastUpdate(System.currentTimeMillis());
        } catch (Exception e) {
            log.error("register reput error.", e);
            res.put("code", -9999);
            res.put("message", "register reput error: " + e.getMessage());
            return res;
        }

        res.put("code", 0);
        res.put("message", "success");
        return res;
    }

    public Pair<MessageView, List<MessageTrack>> viewMessage(String subject, final String msgId) {
        try {
            MessageExt messageExt = null;
            Set<Long> keySet = reputCache.keySet();

            for (Long k : keySet) {
                ReputNode node = reputCache.get(k);
                String url = new StringBuilder(node.getUrl()).append("/message/id/").append(subject).append("/").append(msgId).toString();
                ResponseData responseData = HttpClientUtil.get(url, new HashMap<>(), ResponseData.class);
                if (responseData != null && responseData.getData() != null) {

                    messageExt = map2MessageExt((Map<String, Object>) responseData.getData());
                    if (StringUtils.isNotBlank(messageExt.getMsgId())) {
                        break;
                    }
                }
            }

            List<MessageTrack> messageTrackList = messageTrackDetail(messageExt);
            return new Pair<>(MessageView.fromMessageExtAndDecode(messageExt), messageTrackList);
        } catch (Exception e) {
            throw new ServiceException(-1, String.format("Failed to query message by Id: %s", msgId));
        }
    }

    @Override
    public List<MessageView> queryMessageByTopicAndKey(String topic, String key) {
        try {
            Set<Long> keySet = reputCache.keySet();

            List<MessageView> res = new ArrayList<>();
            List<MessageExt> messageExtList = new ArrayList<>();
            for (Long k : keySet) {
                ReputNode node = reputCache.get(k);
                String url = new StringBuilder(node.getUrl()).append("/message/key/").append(topic).append("/").append(key).toString();

                res.addAll(viewMessageList(url));
            }

            return res;
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    @Override
    public List<MessageView> queryMessageByTopic(String topic, final long begin, final long end) {

        List<MessageView> result = new ArrayList<>();

        List<ReputNode> reputNodeList = getReputNodeList(begin, end);

        if (reputNodeList.isEmpty()) {
            return result;
        } else if (reputNodeList.size() == 1) {
            ReputNode node = reputNodeList.get(0);

            String url = new StringBuilder(node.getUrl()).append("/message/view/").append(topic).append("/").append(begin).append("/").append(end).toString();
            result.addAll(viewMessageList(url));
        } else if (reputNodeList.size() == 2) {

            Collections.sort(reputNodeList, ((o1, o2) -> Long.valueOf(o2.getStartTime()).compareTo(Long.valueOf(o1.getStartTime()))));
            ReputNode node1 = reputNodeList.get(0);
            String url1 = new StringBuilder(node1.getUrl()).append("/message/view/").append(topic).append("/").append(begin).append("/").append(System.currentTimeMillis() - node1.endTime).toString();
            result.addAll(viewMessageList(url1));

            ReputNode node2 = reputNodeList.get(1);
            String url2 = new StringBuilder(node2.getUrl()).append("/message/view/").append(topic).append("/").append(System.currentTimeMillis() - node2.getStartTime()).append("/").append(end).toString();
            result.addAll(viewMessageList(url2));
        }

        Collections.sort(result, (o1, o2) -> {
            if (o1.getStoreTimestamp() - o2.getStoreTimestamp() == 0) {
                return 0;
            }
            return (o1.getStoreTimestamp() > o2.getStoreTimestamp()) ? -1 : 1;
        });
        return result;
    }

    @Override public Object queryMessageTotalByTopic(String topic, long begin, long end) {
        Map<String, Object> res = new HashMap<>();

        Integer total = 0;

        List<ReputNode> reputNodeList = getReputNodeList(begin, end);

        if (reputNodeList.isEmpty()) {
            ;
        } else if (reputNodeList.size() == 1) {
            ReputNode node = reputNodeList.get(0);

            String url = new StringBuilder(node.getUrl()).append("/message/total/").append(topic).append("/").append(begin).append("/").append(end).toString();
            total += getMessageTotal(url);
        } else if (reputNodeList.size() == 2) {

            Collections.sort(reputNodeList, ((o1, o2) -> Long.valueOf(o2.getStartTime()).compareTo(Long.valueOf(o1.getStartTime()))));
            ReputNode node1 = reputNodeList.get(0);
            String url1 = new StringBuilder(node1.getUrl()).append("/message/total/").append(topic).append("/").append(begin).append("/").append(System.currentTimeMillis() - node1.endTime).toString();
            total += getMessageTotal(url1);

            ReputNode node2 = reputNodeList.get(1);
            String url2 = new StringBuilder(node2.getUrl()).append("/message/total/").append(topic).append("/").append(System.currentTimeMillis() - node2.getStartTime()).append("/").append(end).toString();
            total += getMessageTotal(url2);
        }
        res.put("total", total);
        return res;
    }

    @Override public List<MessageTrack> messageTrackDetail(MessageExt msg) {
        try {
            return mqAdminExt.messageTrackDetail(msg);
        } catch (Exception e) {
            log.error("op=messageTrackDetailError", e);
            return Collections.emptyList();
        }
    }

    private List<ReputNode> getReputNodeList(final long begin, final long end) {
        List<ReputNode> reputNodeList = new ArrayList<>();
        Set<Long> keySet = reputCache.keySet();
        if (keySet.isEmpty()) {
            return reputNodeList;
        } else if (keySet.size() == 1) {
            reputNodeList.add(reputCache.get(keySet.stream().findFirst().get()));
        } else {
            for (Long startTime : keySet) {
                ReputNode node = reputCache.get(startTime);
                if (node == null) {
                    continue;
                }
                if (begin >= System.currentTimeMillis() - node.getStartTime() && end <= System.currentTimeMillis() - node.getEndTime()) {
                    reputNodeList.clear();
                    reputNodeList.add(node);
                    break;
                } else if (begin >= System.currentTimeMillis() - node.getStartTime() && begin <= System.currentTimeMillis() - node.getEndTime()) {
                    reputNodeList.add(node);
                } else if (end >= System.currentTimeMillis() - node.getStartTime() && end <= System.currentTimeMillis() - node.getEndTime()) {
                    reputNodeList.add(node);
                } else {
                    continue;
                }

            }
        }
        return reputNodeList;
    }

    private MessageExt map2MessageExt(final Map<String, Object> map) {
        MessageExt messageExt = new MessageExt();
        List<Field> fieldList = new ArrayList<>();
        for (Class<?> cls = messageExt.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
            fieldList.addAll(Arrays.asList(cls.getDeclaredFields()));
        }
        for (Field field : fieldList) {

            String name = field.getName();
            if (map.containsKey(name)) {

                boolean isAccessible = field.isAccessible();
                field.setAccessible(true);
                if (field.getType() == SocketAddress.class) {
                    String value = (String) map.get(name);
                    String[] arr = value.split(":");
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(arr[0], Integer.valueOf(arr[1]));
                    try {
                        field.set(messageExt, inetSocketAddress);
                    } catch (IllegalAccessException e) {
                        log.warn("IllegalAccessException", e);
                    }
                } else if (field.getType() == byte[].class) {
                    try {
                        field.set(messageExt, String.valueOf(map.get(name)).getBytes());
                    } catch (IllegalAccessException e) {
                        log.warn("IllegalAccessException", e);
                    }
                } else {
                    try {
                        field.set(messageExt, map.get(name));
                    } catch (IllegalAccessException e) {
                        log.warn("IllegalAccessException", e);
                    }
                }
                field.setAccessible(isAccessible);
            }
        }
        return messageExt;
    }

    private Integer getMessageTotal(String url) {
        try {
            ResponseData responseData = HttpClientUtil.get(url, new HashMap<>(), ResponseData.class);
            return (Integer) responseData.getData();
        } catch (IOException e) {
            log.error("getMessageTotal error", e);
            Throwables.propagate(e);
        }
        return 0;
    }

    private List<MessageView> viewMessageList(String url) {
        try {
            ResponseData responseData = HttpClientUtil.get(url, new HashMap<>(), ResponseData.class);
            List<Map<String, Object>> messageExts = (List<Map<String, Object>>) responseData.getData();
            List<MessageView> messageViewListByQuery = Lists.transform(messageExts, messageExt -> MessageView.fromMap(messageExt));
            return messageViewListByQuery;
        } catch (IOException e) {
            log.error("queryMessageByTopic error", e);
            Throwables.propagate(e);
        }
        return Collections.EMPTY_LIST;
    }

    private String getInstanceName() {
        StringBuilder sb = new StringBuilder();
        sb.append(RemotingUtil.getLocalAddress()).append("@").append(UtilAll.getPid()).append("#").append(instanceNum.getAndIncrement());
        String instanceName = sb.toString();
        log.info("Generate consumer instance name {}", instanceName);
        return instanceName;
    }

    @Data
    @AllArgsConstructor
    static class ReputNode {
        long startTime;
        long endTime;
        String url;
        long lastUpdate;
    }

    private long getMillByHour(int h) {
        return h * 3600 * 1000;
    }

    static class ThreadSafeTreeMap<K, V> {

        private final Map<K, V> innerMap;

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        ThreadSafeTreeMap(Comparator<? super K> comparator) {
            innerMap = new TreeMap<>(comparator);
        }

        public void put(K k, V v) {
            try {
                writeLock.lock();
                innerMap.put(k, v);
            } finally {
                writeLock.unlock();
            }
        }

        public V get(K k) {
            try {
                readLock.lock();
                return innerMap.get(k);
            } finally {
                readLock.unlock();
            }
        }

        public V remove(K k) {
            try {
                writeLock.lock();
                return innerMap.remove(k);
            } finally {
                writeLock.unlock();
            }
        }

        public Set<K> keySet() {
            try {
                readLock.lock();
                return innerMap.keySet();
            } finally {
                readLock.unlock();
            }
        }

        public boolean containsKey(K k) {
            try {
                readLock.lock();
                return innerMap.containsKey(k);
            } finally {
                readLock.unlock();
            }
        }
    }
}
