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
 * @date 2021-04-16 17:23:54
 * @description rocketmq-console-ng
 **/
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ApproveTopicEvent extends ApplicationEvent {

    private Long applyId;

    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with which the event is associated (never {@code
     *               null})
     */
    public ApproveTopicEvent(Object source, Long applyId) {
        super(source);
        this.applyId = applyId;
    }
}
