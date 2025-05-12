package org.apache.rocketmq.console.service;

import com.github.pagehelper.PageInfo;
import org.apache.rocketmq.console.model.AccountOperationRecord;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-13 15:52:03
 * @description rocketmq-console-ng
 **/
public interface AccountOperationRecordService {

    int add(AccountOperationRecord record);

    PageInfo select(AccountOperationRecord record, int page, int limit);

    Object resend(AccountOperationRecord record);
}
