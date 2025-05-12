package org.apache.rocketmq.console.controller;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import org.apache.rocketmq.common.AclConfig;
import org.apache.rocketmq.common.PlainAccessConfig;
import org.apache.rocketmq.console.interceptor.security.NotAdminDisable;
import org.apache.rocketmq.console.model.AclBelongInfo;
import org.apache.rocketmq.console.model.request.AclRequest;
import org.apache.rocketmq.console.service.AclService;
import org.apache.rocketmq.console.support.JsonResult;
import org.apache.rocketmq.console.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author xuxd
 * @Date 2020-12-11 15:15:20
 * @Description rocketmq-console-ng
 **/
@RestController
@RequestMapping("/acl")
@NotAdminDisable
public class AclController {

    @Autowired
    private AclService aclService;

    @GetMapping("/config.query")
    public AclConfig getAclConfig() {
        return aclService.getAclConfig();
    }

    @PostMapping("/add.do")
    public Object addAclConfig(@RequestBody PlainAccessConfig config) {
        Preconditions.checkArgument(config.getAccessKey() != null && !config.getAccessKey().isEmpty(), "accessKey is null");
        Preconditions.checkArgument(config.getSecretKey() != null && !config.getSecretKey().isEmpty(), "secretKey is null");
        aclService.addAclConfig(config);
        return new JsonResult(0, "");
    }

    @PostMapping("/delete.do")
    public Object deleteAclConfig(@RequestBody PlainAccessConfig config) {
        Preconditions.checkArgument(config.getAccessKey() != null && !config.getAccessKey().isEmpty(), "accessKey is null");
        aclService.deleteAclConfig(config);
        return new JsonResult(0, "");
    }

    @PostMapping("/update.do")
    public Object updateAclConfig(@RequestBody PlainAccessConfig config) {
        Preconditions.checkArgument(config.getSecretKey() != null && !config.getSecretKey().isEmpty(), "secretKey is null");
        aclService.updateAclConfig(config);
        return new JsonResult(0, "");
    }

    @PostMapping("/topic/add.do")
    public Object addAclTopicConfig(@RequestBody AclRequest request) {
        Preconditions.checkArgument(request.getConfig().getAccessKey() != null && !request.getConfig().getAccessKey().isEmpty(), "accessKey is null");
        Preconditions.checkArgument(request.getConfig().getSecretKey() != null && !request.getConfig().getSecretKey().isEmpty(), "secretKey is null");
        Preconditions.checkArgument(request.getConfig().getTopicPerms() != null && !request.getConfig().getTopicPerms().isEmpty(), "topic perms is null");
        Preconditions.checkArgument(request.getTopicPerm() != null && !request.getTopicPerm().isEmpty(), "topic perm is null");
        aclService.addOrUpdateAclTopicConfig(request);
        return new JsonResult(0, "");
    }

    @PostMapping("/group/add.do")
    public Object addAclGroupConfig(@RequestBody AclRequest request) {
        Preconditions.checkArgument(request.getConfig().getAccessKey() != null && !request.getConfig().getAccessKey().isEmpty(), "accessKey is null");
        Preconditions.checkArgument(request.getConfig().getSecretKey() != null && !request.getConfig().getSecretKey().isEmpty(), "secretKey is null");
        Preconditions.checkArgument(request.getConfig().getGroupPerms() != null && !request.getConfig().getGroupPerms().isEmpty(), "group perms is null");
        Preconditions.checkArgument(request.getGroupPerm() != null && !request.getGroupPerm().isEmpty(), "group perm is null");
        aclService.addOrUpdateAclGroupConfig(request);
        return new JsonResult(0, "");
    }

    @PostMapping("/perm/delete.do")
    public Object deletePermConfig(@RequestBody AclRequest request) {
        Preconditions.checkArgument(request.getConfig().getAccessKey() != null && !request.getConfig().getAccessKey().isEmpty(), "accessKey is null");
        Preconditions.checkArgument(request.getConfig().getSecretKey() != null && !request.getConfig().getSecretKey().isEmpty(), "secretKey is null");
        aclService.deletePermConfig(request);
        return new JsonResult(0, "");
    }

    @PostMapping("/sync.do")
    public Object syncConfig(@RequestBody PlainAccessConfig config) {
        Preconditions.checkArgument(config.getAccessKey() != null && !config.getAccessKey().isEmpty(), "accessKey is null");
        Preconditions.checkArgument(config.getSecretKey() != null && !config.getSecretKey().isEmpty(), "secretKey is null");
        aclService.syncData(config);
        return new JsonResult(0, "");
    }

    @PostMapping("/white/list/add.do")
    public Object addWhiteList(@RequestBody List<String> whiteList) {
        Preconditions.checkArgument(whiteList != null && !whiteList.isEmpty(), "white list is null");
        aclService.addWhiteList(whiteList);
        return new JsonResult(0, "");
    }

    @DeleteMapping("/white/list/delete.do")
    public Object deleteWhiteAddr(@RequestParam String request) {
        aclService.deleteWhiteAddr(request);
        return new JsonResult(0, "");
    }

    @PostMapping("/white/list/sync.do")
    public Object synchronizeWhiteList(@RequestBody List<String> whiteList) {
        Preconditions.checkArgument(whiteList != null && !whiteList.isEmpty(), "white list is null");
        aclService.synchronizeWhiteList(whiteList);
        return new JsonResult(0, "");
    }

    @PostMapping("/belong/item/add.do")
    public Object addAclBelongInfo(@RequestBody AclBelongInfo info) {
        Objects.requireNonNull(info.getAccessKey(), "access key is null");
        aclService.insertBelongInfo(info);
        return new JsonResult(0, "");
    }

    @GetMapping("/belong/item/group.query")
    public Object selectAclBelongInfoByItem(@RequestParam(required = false) Integer itemId) {
        return new JsonResult<>(aclService.selectAclBelongInfoByItem(itemId == null ? WebUtil.getLoginInfo().getUser().getItem().getId() : itemId));
    }
}
