package org.apache.rocketmq.console.model.dao;

import lombok.Data;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 19:25:40
 * @description rocketmq-console-ng
 **/
@Data
public class SubItemInfoDO {

    private Long id;

    private String subItemCode;

    private String subItemName;

    private String description;

    private Long itemId;

    private String username;
}
