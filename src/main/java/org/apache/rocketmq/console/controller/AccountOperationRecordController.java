package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.console.interceptor.security.NotAdminDisable;
import org.apache.rocketmq.console.model.AccountOperationRecord;
import org.apache.rocketmq.console.service.AccountOperationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-13 19:26:01
 * @description rocketmq-console-ng
 **/
@RestController
@RequestMapping("/aor")
@NotAdminDisable
public class AccountOperationRecordController {

    @Autowired
    private AccountOperationRecordService accountOperationRecordService;

    @GetMapping
    public Object select(@RequestParam(required = false) String username, @RequestParam int page,
        @RequestParam int limit) {
        AccountOperationRecord record = new AccountOperationRecord();
        record.setUsername(username);
        return accountOperationRecordService.select(record, page, limit);
    }

    @PostMapping("/resend.do")
    public Object select(@RequestBody AccountOperationRecord record) {
        return accountOperationRecordService.resend(record);
    }
}
