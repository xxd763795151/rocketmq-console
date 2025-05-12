package org.apache.rocketmq.console.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * @Author xuxd
 * @Date 2:12 下午 2020/11/18
 * @Description LogUtil
 **/
public class LogUtil {

    public static void statsErrorLog(Logger log, String des, Exception e) {
        String errorMessage = e.getMessage();
        if (StringUtils.isNotEmpty(errorMessage) && errorMessage.contains("not exist")) {
            log.error(des + ": {}", errorMessage);
        } else {
            log.error(des, e);
        }
    }
}
