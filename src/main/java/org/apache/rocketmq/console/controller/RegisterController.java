package org.apache.rocketmq.console.controller;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.console.service.BackupMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-07-05 15:00:07
 **/
@Slf4j
@RestController
@RequestMapping("/register")
public class RegisterController {

    @Autowired
    private BackupMessageService backupMessageService;

    @PostMapping("/reput")
    public Object registerReput(@RequestBody Map<String, String> params) {
        return backupMessageService.registerReput(params);
    }
}
