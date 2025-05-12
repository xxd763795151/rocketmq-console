package org.apache.rocketmq.console.service;

import java.util.List;
import org.apache.rocketmq.console.model.BelongItem;
import org.apache.rocketmq.console.model.Item;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-01-25 20:23:35
 * @description rocketmq-console-ng
 **/
public interface BelongItemService {
    List<BelongItem> selectByItemList(List<Item> items);
}
