package org.apache.rocketmq.console.model;

import java.util.List;

/**
 * @Author xuxd
 * @Date 9:51 下午 2020/11/17
 * @Description NotifyConfigElement
 **/
public class NotifyConfigElement {
    private String type;
    // notify into: 1, topic, 2, consumer, 3, topic & consumer
    private int info;
    private List<String> topics;
    private List<String> consumers;
    private List<String> concat;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getInfo() {
        return info;
    }

    public void setInfo(int info) {
        this.info = info;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<String> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<String> consumers) {
        this.consumers = consumers;
    }

    public List<String> getConcat() {
        return concat;
    }

    public void setConcat(List<String> concat) {
        this.concat = concat;
    }

    @Override
    public String toString() {
        return "NotifyConfigElement{" +
                "type='" + type + '\'' +
                ", info=" + info +
                ", topics=" + topics +
                ", consumers=" + consumers +
                ", concat=" + concat +
                '}';
    }
}
