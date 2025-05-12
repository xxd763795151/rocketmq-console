package org.apache.rocketmq.console.model;

import java.util.List;

/**
 * @Author xuxd
 * @Date 2020-12-17 17:53:41
 * @Description rocketmq-console-ng
 **/
public class AclBelongInfo {

    private long id;
    private String accessKey;
    private Long itemId;

    // The definition of AclBelongInfo is not canonical, just for convenience.
    private Item item;
    private List<Item> items;
    private List<Long> itemIdList;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<Long> getItemIdList() {
        return itemIdList;
    }

    public void setItemIdList(List<Long> itemIdList) {
        this.itemIdList = itemIdList;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "AclBelongInfo{" +
                "id=" + id +
                ", accessKey='" + accessKey + '\'' +
                ", itemId=" + itemId +
                ", item=" + item +
                ", items=" + items +
                ", itemIdList=" + itemIdList +
                '}';
    }
}
