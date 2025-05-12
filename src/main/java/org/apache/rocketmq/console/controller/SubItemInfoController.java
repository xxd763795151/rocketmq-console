package org.apache.rocketmq.console.controller;

import java.util.Objects;
import org.apache.rocketmq.console.interceptor.security.NotAdminDisable;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.SubItemInfoRequest;
import org.apache.rocketmq.console.service.SubItemInfoService;
import org.apache.rocketmq.console.util.WebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 19:59:21
 * @description rocketmq-console-ng
 **/
@RestController
@RequestMapping("/sub/item")
public class SubItemInfoController {

    @Autowired
    private SubItemInfoService subItemInfoService;

    @PostMapping
    public Object add(@RequestBody SubItemInfoRequest request) {
        Objects.requireNonNull(request.getSubItemCode(), "getSubItemCode is null");
        Objects.requireNonNull(request.getSubItemName(), "getSubItemName is null");
        User user = WebUtil.getLoginInfo().getUser();
        request.setItemId(user.getItem().getId());
        request.setUsername(user.getName());
        return subItemInfoService.add(request);
    }

    @GetMapping
    public Object select(SubItemInfoRequest request) {
        User user = WebUtil.getLoginInfo().getUser();
        if (!user.isAdmin()) {
            request.setItemId(user.getItem().getId());
        }
        return subItemInfoService.select(request);
    }

    @PutMapping
    public Object update(@RequestBody SubItemInfoRequest request) {
        Objects.requireNonNull(request.getId(), "getId is null");
        return subItemInfoService.update(request);
    }

    @NotAdminDisable
    @DeleteMapping
    public Object delete(SubItemInfoRequest request) {
        Objects.requireNonNull(request.getId(), "getId is null");
        return subItemInfoService.delete(request);
    }

    @GetMapping("/group")
    public Object selectGroupByItem(SubItemInfoRequest request) {
        User user = WebUtil.getLoginInfo().getUser();
        request.setItemId(user.getItem().getId());
        return subItemInfoService.selectGroupByItem(request);
    }
}
