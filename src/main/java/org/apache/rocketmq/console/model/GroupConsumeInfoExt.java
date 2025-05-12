package org.apache.rocketmq.console.model;

import java.util.List;

/**
 * @Author xuxd
 * @Date 2020-12-09 14:07:32
 * @Description rocketmq-console-ng
 **/
public class GroupConsumeInfoExt extends GroupConsumeInfo {

    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
