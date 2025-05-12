package org.apache.rocketmq.console.model;

import org.apache.rocketmq.common.PlainAccessConfig;

import java.util.List;

/**
 * @Author xuxd
 * @Date 2020-12-18 10:50:24
 * @Description rocketmq-console-ng
 **/
public class PlainAccessConfigEx extends PlainAccessConfig {

    List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PlainAccessConfigEx{" +
                "items=" + items +
                "} " + super.toString();
    }
}
