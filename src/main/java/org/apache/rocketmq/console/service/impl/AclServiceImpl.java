package org.apache.rocketmq.console.service.impl;

import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.AclConfig;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.PlainAccessConfig;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.dao.AclBelongItemMapper;
import org.apache.rocketmq.console.model.AclBelongInfo;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.PlainAccessConfigEx;
import org.apache.rocketmq.console.model.request.AclRequest;
import org.apache.rocketmq.console.service.AbstractCommonService;
import org.apache.rocketmq.console.service.AclService;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author xuxd
 * @Date 2020-12-11 14:25:50
 * @Description rocketmq-console-ng
 **/
@Service
public class AclServiceImpl extends AbstractCommonService implements AclService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AclServiceImpl.class);

    @Autowired
    private AclBelongItemMapper aclBelongItemMapper;

    @Autowired
    private RMQConfigure configure;

    @Override
    public AclConfig getAclConfig() {
        try {
            Set<String> masterSet = getMasterSet();
            Optional<String> addr = masterSet.stream().findFirst();
            if (addr.isPresent()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr.get());
                //associated item info
                if (aclConfig.getPlainAccessConfigs() != null && !aclConfig.getPlainAccessConfigs().isEmpty()) {
                    List<PlainAccessConfig> remoteConfig = aclConfig.getPlainAccessConfigs();
                    List<String> accessKeyList = remoteConfig.stream().map(PlainAccessConfig::getAccessKey).collect(Collectors.toList());
                    List<AclBelongInfo> aclBelongInfos = aclBelongItemMapper.selectAclBelongInfosByAccessKey(accessKeyList);
                    if (aclBelongInfos != null && !aclBelongInfos.isEmpty()) {
                        Map<String, List<Item>> map = new HashMap<>();
                        aclBelongInfos.stream().forEach(info -> {
                            List<Item> itemList = map.getOrDefault(info.getAccessKey(), new ArrayList<>());
                            itemList.add(info.getItem());
                            map.put(info.getAccessKey(), itemList);
                        });
                        List<PlainAccessConfig> associatedItemConfig = new ArrayList<>();
                        remoteConfig.stream().forEach(config -> {
                            PlainAccessConfigEx plainAccessConfigEx = new PlainAccessConfigEx();
                            BeanUtils.copyProperties(config, plainAccessConfigEx);
                            plainAccessConfigEx.setItems(map.get(config.getAccessKey()));
                            associatedItemConfig.add(plainAccessConfigEx);
                        });
                        aclConfig.setPlainAccessConfigs(associatedItemConfig);
                    }
                }
                return aclConfig;
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        AclConfig aclConfig = new AclConfig();
        aclConfig.setGlobalWhiteAddrs(Collections.EMPTY_LIST);
        aclConfig.setPlainAccessConfigs(Collections.emptyList());
        return aclConfig;
    }

    @Override
    public void addAclConfig(PlainAccessConfig config) {
        try {
            Set<String> masterSet = getMasterSet();

            if (masterSet.isEmpty()) {
                throw new IllegalStateException("broker addr list is empty");
            }
            // check to see if account is exists
            for (String addr :
                masterSet) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                List<PlainAccessConfig> plainAccessConfigs = aclConfig.getPlainAccessConfigs();
                for (PlainAccessConfig pac : plainAccessConfigs) {
                    if (pac.getAccessKey().equals(config.getAccessKey())) {
                        throw new IllegalArgumentException(String.format("broker: %s，exist accessKey: %s", addr, config.getAccessKey()));
                    }
                }
            }

            // all broker
            for (String addr : getBrokerAddrs()) {
                mqAdminExt.createAndUpdatePlainAccessConfig(addr, config);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    public void deleteAclConfig(PlainAccessConfig config) {
        try {
            for (String addr : getBrokerAddrs()) {
                LOGGER.info("Start to delete acl [{}] from broker [{}]", config.getAccessKey(), addr);
                if (isExistAccessKey(config.getAccessKey(), addr)) {
                    mqAdminExt.deletePlainAccessConfig(addr, config.getAccessKey());
                }
                LOGGER.info("Delete acl [{}] from broker [{}] complete", config.getAccessKey(), addr);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void updateAclConfig(PlainAccessConfig config) {
        try {
            for (String addr : getBrokerAddrs()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                if (aclConfig.getPlainAccessConfigs() != null) {
                    PlainAccessConfig remoteConfig = null;
                    for (PlainAccessConfig pac : aclConfig.getPlainAccessConfigs()) {
                        if (pac.getAccessKey().equals(config.getAccessKey())) {
                            remoteConfig = pac;
                            break;
                        }
                    }
                    if (remoteConfig != null) {
                        remoteConfig.setSecretKey(config.getSecretKey());
                        remoteConfig.setAdmin(config.isAdmin());
                        config = remoteConfig;
                    }
                }
                mqAdminExt.createAndUpdatePlainAccessConfig(addr, config);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addOrUpdateAclTopicConfig(AclRequest request) {
        try {
            PlainAccessConfig addConfig = request.getConfig();
            for (String addr : getBrokerAddrs()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                PlainAccessConfig remoteConfig = null;
                if (aclConfig.getPlainAccessConfigs() != null) {
                    for (PlainAccessConfig config : aclConfig.getPlainAccessConfigs()) {
                        if (config.getAccessKey().equals(addConfig.getAccessKey())) {
                            remoteConfig = config;
                            break;
                        }
                    }
                }
                if (remoteConfig == null) {
                    // May be the broker no acl config of the access key, therefore add it;
                    mqAdminExt.createAndUpdatePlainAccessConfig(addr, addConfig);
                } else {
                    if (remoteConfig.getTopicPerms() == null) {
                        remoteConfig.setTopicPerms(new ArrayList<>());
                    }
                    removeExist(remoteConfig.getTopicPerms(), request.getTopicPerm().split("=")[0]);
                    remoteConfig.getTopicPerms().add(request.getTopicPerm());
                    mqAdminExt.createAndUpdatePlainAccessConfig(addr, remoteConfig);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addOrUpdateAclGroupConfig(AclRequest request) {
        try {
            PlainAccessConfig addConfig = request.getConfig();
            for (String addr : getBrokerAddrs()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                PlainAccessConfig remoteConfig = null;
                if (aclConfig.getPlainAccessConfigs() != null) {
                    for (PlainAccessConfig config : aclConfig.getPlainAccessConfigs()) {
                        if (config.getAccessKey().equals(addConfig.getAccessKey())) {
                            remoteConfig = config;
                            break;
                        }
                    }
                }
                if (remoteConfig == null) {
                    // May be the broker no acl config of the access key, therefore add it;
                    mqAdminExt.createAndUpdatePlainAccessConfig(addr, addConfig);
                } else {
                    if (remoteConfig.getGroupPerms() == null) {
                        remoteConfig.setGroupPerms(new ArrayList<>());
                    }
                    removeExist(remoteConfig.getGroupPerms(), request.getGroupPerm().split("=")[0]);
                    remoteConfig.getGroupPerms().add(request.getGroupPerm());
                    mqAdminExt.createAndUpdatePlainAccessConfig(addr, remoteConfig);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void deletePermConfig(AclRequest request) {
        try {
            PlainAccessConfig deleteConfig = request.getConfig();

            String topic = request.getTopicPerm() != null && !request.getTopicPerm().isEmpty() ? request.getTopicPerm().split("=")[0] : null;
            String group = request.getGroupPerm() != null && !request.getGroupPerm().isEmpty() ? request.getGroupPerm().split("=")[0] : null;
            if (deleteConfig.getTopicPerms() != null && topic != null) {
                removeExist(deleteConfig.getTopicPerms(), topic);
            }
            if (deleteConfig.getGroupPerms() != null && group != null) {
                removeExist(deleteConfig.getGroupPerms(), group);
            }

            for (String addr : getBrokerAddrs()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                PlainAccessConfig remoteConfig = null;
                if (aclConfig.getPlainAccessConfigs() != null) {
                    for (PlainAccessConfig config : aclConfig.getPlainAccessConfigs()) {
                        if (config.getAccessKey().equals(deleteConfig.getAccessKey())) {
                            remoteConfig = config;
                            break;
                        }
                    }
                }
                if (remoteConfig == null) {
                    // May be the broker no acl config of the access key, therefore add it;
                    mqAdminExt.createAndUpdatePlainAccessConfig(addr, deleteConfig);
                } else {
                    if (remoteConfig.getTopicPerms() != null && topic != null) {
                        removeExist(remoteConfig.getTopicPerms(), topic);
                    }
                    if (remoteConfig.getGroupPerms() != null && group != null) {
                        removeExist(remoteConfig.getGroupPerms(), group);
                    }
                    mqAdminExt.createAndUpdatePlainAccessConfig(addr, remoteConfig);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    public void syncData(PlainAccessConfig config) {
        try {
            for (String addr : getBrokerAddrs()) {
                mqAdminExt.createAndUpdatePlainAccessConfig(addr, config);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addWhiteList(List<String> whiteList) {
        if (whiteList == null) {
            return;
        }
        try {
            for (String addr : getBrokerAddrs()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                if (aclConfig.getGlobalWhiteAddrs() != null) {
                    aclConfig.setGlobalWhiteAddrs(Stream.of(whiteList, aclConfig.getGlobalWhiteAddrs()).flatMap(Collection::stream).distinct().collect(Collectors.toList()));
                } else {
                    aclConfig.setGlobalWhiteAddrs(whiteList);
                }
                mqAdminExt.updateGlobalWhiteAddrConfig(addr, UtilAll.list2String(aclConfig.getGlobalWhiteAddrs(), ","));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void deleteWhiteAddr(String deleteAddr) {
        try {
            for (String addr : getBrokerAddrs()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                if (aclConfig.getGlobalWhiteAddrs() == null || aclConfig.getGlobalWhiteAddrs().isEmpty()) {
                    continue;
                }
                aclConfig.getGlobalWhiteAddrs().remove(deleteAddr);
                mqAdminExt.updateGlobalWhiteAddrConfig(addr, UtilAll.list2String(aclConfig.getGlobalWhiteAddrs(), ","));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void synchronizeWhiteList(List<String> whiteList) {
        if (whiteList == null) {
            return;
        }
        try {
            for (String addr : getBrokerAddrs()) {
                mqAdminExt.updateGlobalWhiteAddrConfig(addr, UtilAll.list2String(whiteList, ","));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void insertBelongInfo(AclBelongInfo info) {
        aclBelongItemMapper.deleteBelongInfoByAccessKey(info.getAccessKey());
        if (info.getItemIdList() != null && !info.getItemIdList().isEmpty()) {
            aclBelongItemMapper.insertAclBelongInfo(info);
        }
    }

    @Override public List<AclBelongInfo> selectAclBelongInfoByItem(Long itemId) {
        return aclBelongItemMapper.selectAclBelongInfoByItem(itemId);
    }

    @Override public Optional<PlainAccessConfig> getPlainAccessConfigByAccessKey(String accessKey) {
        try {
            for (String addr : getMasterSet()) {
                AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
                List<PlainAccessConfig> plainAccessConfigs = aclConfig.getPlainAccessConfigs();
                if (CollectionUtils.isEmpty(plainAccessConfigs)) {
                    return Optional.empty();
                }
                for (PlainAccessConfig config : plainAccessConfigs) {
                    if (config.getAccessKey().equals(accessKey)) {
                        return Optional.of(config);
                    }
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return Optional.empty();
    }

    private void removeExist(List<String> list, String name) {
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String v = iterator.next();
            String cmp = v.split("=")[0];
            if (cmp.equals(name)) {
                iterator.remove();
            }
        }
    }

    private boolean isExistAccessKey(String accessKey,
        String addr) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        AclConfig aclConfig = mqAdminExt.examineBrokerClusterAclConfig(addr);
        List<PlainAccessConfig> plainAccessConfigs = aclConfig.getPlainAccessConfigs();
        if (plainAccessConfigs == null || plainAccessConfigs.isEmpty()) {
            return false;
        }
        for (PlainAccessConfig config : plainAccessConfigs) {
            if (accessKey.equals(config.getAccessKey())) {
                return true;
            }
        }
        return false;
    }

    private Set<BrokerData> getBrokerDataSet() throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException, MQBrokerException {
        Set<BrokerData> brokerDataSet = new HashSet<>();
        ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        Map<String, BrokerData> brokerDataMap = clusterInfo.getBrokerAddrTable();
        if (!configure.getTraceNode().isAclEnable()) {
            brokerDataSet.addAll(brokerDataMap.entrySet().stream().filter(entry -> !entry.getKey().startsWith(configure.getTraceNode().getPrefix())).map(entry -> entry.getValue()).collect(Collectors.toList()));
        } else {
            brokerDataSet.addAll(brokerDataMap.values());
        }
        return brokerDataSet;
    }

    private Set<String> getMasterSet() throws InterruptedException, MQBrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        Set<String> masterSet = new HashSet<>();
        masterSet.addAll(getBrokerDataSet().stream().map(data -> data.getBrokerAddrs().get(MixAll.MASTER_ID)).collect(Collectors.toSet()));
        return masterSet;
    }

    private Set<String> getBrokerAddrs() throws InterruptedException, MQBrokerException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        Set<String> brokerAddrs = new HashSet<>();
        getBrokerDataSet().stream().forEach(data -> brokerAddrs.addAll(data.getBrokerAddrs().values()));
        return brokerAddrs;
    }

}
