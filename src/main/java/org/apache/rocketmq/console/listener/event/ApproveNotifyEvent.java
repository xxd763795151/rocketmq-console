package org.apache.rocketmq.console.listener.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-21 11:09:13
 * @description rocketmq-console-ng
 **/
@Getter
@EqualsAndHashCode
@ToString
@Setter
public class ApproveNotifyEvent extends ApplicationEvent {

    private String type;

    private Object data;

    private boolean isTopic;

    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with which the event is associated (never
     *               {@code null})
     */
    public ApproveNotifyEvent(Object source, String type, Object data, boolean isTopic) {
        super(source);
        this.type = type;
        this.data = data;
        this.isTopic = isTopic;
    }
}
