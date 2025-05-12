package org.apache.rocketmq.console.model.vo;

import lombok.Data;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 19:25:40
 * @description rocketmq-console-ng
 **/
@Data
public class SubItemInfoVO {

    private Long id;

    private String subItemCode;

    private String subItemName;

    private String description;

    private String itemId;

    private String itemName;

    private String itemCode;

    private String username;
}
