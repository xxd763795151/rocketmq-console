package org.apache.rocketmq.console.service.impl;

import java.util.List;
import org.apache.rocketmq.console.dao.BelongItemMapper;
import org.apache.rocketmq.console.model.BelongItem;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.service.BelongItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-01-25 20:24:04
 * @description rocketmq-console-ng
 **/
@Service
public class BelongItemServiceImpl implements BelongItemService {

    @Autowired
    private BelongItemMapper belongItemMapper;

    @Override public List<BelongItem> selectByItemList(List<Item> items) {
        return belongItemMapper.selectByItemList(items);
    }
}
