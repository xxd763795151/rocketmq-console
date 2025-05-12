package org.apache.rocketmq.console.listener.event;

import org.apache.rocketmq.console.model.User;
import org.springframework.context.ApplicationEvent;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-10 17:37:01
 * @description rocketmq-console-ng
 **/
public class ForgetPasswordEvent extends ApplicationEvent {

    private User user;

    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with which the event is associated (never
     *               {@code null})
     */
    public ForgetPasswordEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
