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
 * @date 2021-04-20 11:47:15
 * @description rocketmq-console-ng
 **/
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ApproveSubscriptionGroupEvent extends ApplicationEvent {

    private Long applyId;
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with which the event is associated (never {@code
     *               null})
     */
    public ApproveSubscriptionGroupEvent(Object source, Long id) {
        super(source);
        this.applyId = id;
    }
}
