create table userinfo
(
    id       bigint(20) unsigned auto_increment comment 'id, primary key',
    username varchar(50) not null DEFAULT '' comment 'user name',
    password varchar(50) not null comment 'user password',
    role_id  bigint      not null DEFAULT 0 comment 'role id',
    item_id  bigint      not null DEFAULT 0 comment 'item id',
    notify   smallint    null     default 0 comment '1: true, 0: false',
    email    varchar(64) null     default '' comment 'notify email',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment ='用户表';

INSERT INTO userinfo (id, username, password, role_id, item_id)
VALUES (1, 'admin', 'admin', 2, 1);

create table role
(
	id int(10) unsigned auto_increment comment 'id, primary key',
	role_name varchar(50) not null DEFAULT '' comment 'role name',
	type smallint default 0 null comment 'role type, 0: normal, 1: admin, 2: allow send...',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uniq_rolename` (`role_name`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='角色表';


create table item
(
	id int(10)  unsigned auto_increment comment 'id, primary key',
	item_name varchar(50) not null comment 'item name',
	code varchar(50) null comment 'item code',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uniq_item_name` (`item_name`)

)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='明细';
INSERT INTO rmq_console.item (id, item_name, code) VALUES (1, '基础架构', 'basic_frame');

create table belong_item
(
	id int(10) unsigned auto_increment comment 'id, primary key',
	type tinyint default 0 not null comment 'type, 1: topic, 2: consumer group',
	topic_consumer_name varchar(100) not null comment 'topic name or consumer group',
	item_id bigint not null comment 'item id',
	PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='明细隶属';


INSERT INTO rmq_console.role (role_name, type) VALUES ('普通用户', 0);
INSERT INTO rmq_console.role (role_name, type) VALUES ('管理员', 1);
INSERT INTO rmq_console.role (role_name, type) VALUES ('允许发送', 2);
INSERT INTO rmq_console.role (role_name, type) VALUES ('看到全部数据', 3);

create table acl_belong_item
(
	id int(10) unsigned auto_increment comment 'id, primary key',
	access_key varchar(100) default 'nil' not null comment 'accessKey of acl account',
	item_id bigint default 0 not null comment 'item id',
	PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='acl明细隶属';

-- alter table userinfo add column notify tinyint default 0 comment 'message notify enable, 0: false, 1: true';
-- alter table userinfo add column email varchar(40) default '' comment 'user email';
alter table userinfo add column notify tinyint default 0 not null comment 'message notify enable, 0: false, 1: true', add column email varchar(40) default '' not null comment 'user email';

-- 这是个操作记录表，不能只通过id与其它表的数据关联，需要数据冗余，因为不涉及修改和更新操作，其它表的数据id即使变动不影响该表历史数据
drop table if exists account_operation_record;
create table account_operation_record
(
    id             bigint(20) unsigned auto_increment comment 'id, primary key, auto increment',
    username       varchar(30)  default '' not null comment 'user name',
    password       varchar(50)  default '' not null comment 'user password',
    email          varchar(50)  default '' not null comment 'user email',
    item_id        int(10)      default 0  not null comment 'item id',
    item_name      varchar(50)  default '' not null comment 'item name',
    role_id        int(10)      default 0  not null comment 'role id',
    role_name      varchar(50)  default '' not null comment 'role name',
    operation_type tinyint      default 0  not null comment 'operation type, 0: register, 1: forget password',
    send_status    tinyint      default 1  not null comment 'email send statue, 1: successful, 0: failed',
    send_message   varchar(200) default '' not null comment 'send message content, json format',
    create_time datetime default now() not null comment 'create time',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment ='账户操作记录';

-- topic申请表
drop table if exists apply_topic;
create table apply_topic
(
    id             bigint(20) unsigned auto_increment not null comment 'primary key, auto increment',
    username       varchar(30)  default ''            not null comment 'user name',
    topic          varchar(100) default ''            not null comment 'topic name',
    producer_group varchar(100) default ''            not null comment 'producer group',
    sub_item       varchar(100) default ''            not null comment 'sub item name or code, different from the item table',
    queue_num      int(10)      default 8             not null comment 'queue quantity',
    apply_remark   varchar(500) default ''            not null comment 'apply remark',
    access_key     varchar(60)  default ''            not null comment 'access key',
    secret_key     varchar(60)  default ''            not null comment 'secret key',
    approve_remark varchar(500) default ''            not null comment 'approve remark',
    apply_stage    tinyint      default 0             not null comment 'apply stage, 0: wait approve, 1: complete',
    apply_result   tinyint      default 1             not null comment 'apply result, 1: allow, 0: reject, 2: unknown',
    send_status    tinyint      default 1             not null comment 'notify status, 1: successful, 0: failed, 2: unknown',
    create_time    datetime     default now()         not null comment 'create time',
    cluster_env    varchar(20)  default 'unknown'     not null comment 'cluster environment',
    PRIMARY KEY (`id`),
    key (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment ='申请topic';

drop table if exists sub_item_info;
create table sub_item_info
(
    id                   bigint(20) unsigned auto_increment not null comment 'primary key, auto increment',
    sub_item_code        varchar(50)  default ''            not null comment 'item code',
    sub_item_name        varchar(60)  default ''            not null comment 'item name',
    sub_item_description varchar(200) default ''            not null comment 'item description',
    item_id              bigint                             not null comment 'item id',
    username             varchar(30)  default ''            not null comment 'user name',
    primary key (`id`),
    unique key (`sub_item_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment ='项目信息，不同于item表，item表是项目团队信息，该表是项目团队下的子项目信息';

drop table if exists apply_subscription_group;
create table apply_subscription_group
(
    id                       bigint(20) unsigned auto_increment not null comment 'primary key, auto increment',
    username                 varchar(30)  default ''            not null comment 'user name',
    topic                    varchar(100) default ''            not null comment 'subscription topic name',
    subscription_group       varchar(100) default ''            not null comment 'subscription group',
    consume_broadcast_enable tinyint      default 0             not null comment 'broadcast consume enable flag, 1: enable, 0: disabled',
    sub_item                 varchar(100) default ''            not null comment 'sub item name or code, different from the item table',
    apply_remark             varchar(500) default ''            not null comment 'apply remark',
    access_key               varchar(60)  default ''            not null comment 'access key',
    secret_key               varchar(60)  default ''            not null comment 'secret key',
    approve_remark           varchar(500) default ''            not null comment 'approve remark',
    apply_stage              tinyint      default 0             not null comment 'apply stage, 0: wait approve, 1: complete',
    apply_result             tinyint      default 1             not null comment 'apply result, 1: allow, 0: reject, 2: unknown',
    send_status              tinyint      default 1             not null comment 'notify status, 1: successful, 0: failed, 2: unknown',
    create_time              datetime     default now()         not null comment 'create time',
    cluster_env              varchar(20)  default 'unknown'     not null comment 'cluster environment',
    PRIMARY KEY (`id`),
    key (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment ='申请订阅组/消费组';