package org.apache.rocketmq.console.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.rocketmq.console.model.AccountOperationRecord;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-13 10:53:41
 * @description rocketmq-console-ng
 **/
@Mapper
public interface AccountOperationRecordMapper {

    int add(AccountOperationRecord record);

    List<AccountOperationRecord> select(AccountOperationRecord record);

    int update(AccountOperationRecord record);
}
