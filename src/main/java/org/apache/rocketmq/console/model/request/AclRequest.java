package org.apache.rocketmq.console.model.request;

import org.apache.rocketmq.common.PlainAccessConfig;

/**
 * @Author xuxd
 * @Date 2020-12-16 11:05:48
 * @Description rocketmq-console-ng
 **/
public class AclRequest {

    private PlainAccessConfig config;
    private String topicPerm;
    private String groupPerm;

    public PlainAccessConfig getConfig() {
        return config;
    }

    public void setConfig(PlainAccessConfig config) {
        this.config = config;
    }

    public String getTopicPerm() {
        return topicPerm;
    }

    public void setTopicPerm(String topicPerm) {
        this.topicPerm = topicPerm;
    }

    public String getGroupPerm() {
        return groupPerm;
    }

    public void setGroupPerm(String groupPerm) {
        this.groupPerm = groupPerm;
    }

    @Override
    public String toString() {
        return "AclRequest{" +
                "config=" + config +
                ", topicPerm='" + topicPerm + '\'' +
                ", groupPerm='" + groupPerm + '\'' +
                '}';
    }
}
