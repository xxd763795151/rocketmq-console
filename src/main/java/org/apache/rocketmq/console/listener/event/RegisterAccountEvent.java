package org.apache.rocketmq.console.listener.event;

import org.apache.rocketmq.console.model.request.UpdateUserRequest;
import org.springframework.context.ApplicationEvent;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-10 17:37:01
 * @description rocketmq-console-ng
 **/
public class RegisterAccountEvent extends ApplicationEvent {

    private UpdateUserRequest user;

    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with which the event is associated (never {@code
     *               null})
     */
    public RegisterAccountEvent(Object source, UpdateUserRequest user) {
        super(source);
        this.user = user;
    }

    public UpdateUserRequest getUser() {
        return user;
    }

    public void setUser(UpdateUserRequest user) {
        this.user = user;
    }
}
