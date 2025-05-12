package org.apache.rocketmq.console.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-10 15:24:17
 * @description rocketmq-console-ng
 **/
@Configuration
@ConfigurationProperties(prefix = "register.controller")
public class RegisterConfig {

    private Long emailInterval;

    private Long clientInterval;

    private boolean emailIntervalCheck;

    private boolean clientIntervalCheck;

    public Long getEmailInterval() {
        return emailInterval;
    }

    public void setEmailInterval(Long emailInterval) {
        this.emailInterval = emailInterval;
    }

    public Long getClientInterval() {
        return clientInterval;
    }

    public void setClientInterval(Long clientInterval) {
        this.clientInterval = clientInterval;
    }

    public boolean isEmailIntervalCheck() {
        return emailIntervalCheck;
    }

    public void setEmailIntervalCheck(boolean emailIntervalCheck) {
        this.emailIntervalCheck = emailIntervalCheck;
    }

    public boolean isClientIntervalCheck() {
        return clientIntervalCheck;
    }

    public void setClientIntervalCheck(boolean clientIntervalCheck) {
        this.clientIntervalCheck = clientIntervalCheck;
    }
}
