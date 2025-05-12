package org.apache.rocketmq.console.model.dao;

import lombok.Data;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-19 10:22:11
 * @description rocketmq-console-ng
 **/
@Data
public class BelongItemDO {

    private Long id;

    private String name;

    private Long itemId;

    private Integer type;
}
