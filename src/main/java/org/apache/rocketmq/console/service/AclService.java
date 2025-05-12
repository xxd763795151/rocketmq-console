package org.apache.rocketmq.console.service;

import java.util.Optional;
import org.apache.rocketmq.common.AclConfig;
import org.apache.rocketmq.common.PlainAccessConfig;
import org.apache.rocketmq.console.model.AclBelongInfo;
import org.apache.rocketmq.console.model.request.AclRequest;

import java.util.List;

/**
 * @Author xuxd
 * @Date 2020-12-11 14:00:53
 * @Description rocketmq-console-ng
 **/
public interface AclService {

    AclConfig getAclConfig();

    void addAclConfig(PlainAccessConfig config);

    void deleteAclConfig(PlainAccessConfig config);

    void updateAclConfig(PlainAccessConfig config);

    void addOrUpdateAclTopicConfig(AclRequest request);

    void addOrUpdateAclGroupConfig(AclRequest request);

    void deletePermConfig(AclRequest request);

    void syncData(PlainAccessConfig config);

    void addWhiteList(List<String> whiteList);

    void deleteWhiteAddr(String addr);

    void synchronizeWhiteList(List<String> whiteList);

    void insertBelongInfo(AclBelongInfo info);

    List<AclBelongInfo> selectAclBelongInfoByItem(Long itemId);

    Optional<PlainAccessConfig> getPlainAccessConfigByAccessKey(String accessKey);
}
