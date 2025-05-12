package org.apache.rocketmq.console.model;

import lombok.Data;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-13 10:49:06
 * @description rocketmq-console-ng
 * <p>
 * create table account_operation_record ( id             bigint(20) unsigned auto_increment comment 'id, primary key,
 * auto increment', username       varchar(30)  default '' not null comment 'user name', password       varchar(50)
 * default '' not null comment 'user password', email          varchar(50)  default '' not null comment 'user email',
 * item_id        int(10)      default 0  not null comment 'item id', item_name      varchar(50)  default '' not null
 * comment 'item name', role_id        int(10)      default 0  not null comment 'role id', role_name      varchar(50)
 * default '' not null comment 'role name', operation_type tinyint      default 0  not null comment 'operation type, 0:
 * register, 1: forget password', send_status    tinyint      default 0  not null comment 'email send statue, 0:
 * successful, 1: failed', send_message   varchar(200) default '' not null comment 'send message content, json format',
 * PRIMARY KEY (`id`) ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 comment ='账户操作记录';
 **/
@Data
public class AccountOperationRecord {

    private Long id;

    private String username;

    private String password;

    private String email;

    private Long itemId;

    private String itemName;

    private Long roleId;

    private String roleName;

    private int operationType;

    private int sendStatus;

    private String sendMessage;

    private String createTime;
}
