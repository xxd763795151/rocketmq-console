package org.apache.rocketmq.console.model;

/**
 * @Author xuxd
 * @Date 2020-12-08 10:34:20
 * @Description rocketmq-console-ng
 **/
public class BelongItem {

    private long id;
    private String name;
    private int type;
    private Item item;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "BelongItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", item=" + item +
                '}';
    }
}
