package org.apache.rocketmq.console.service.client;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.util.BeanUtil;

/**
 * @Author xuxd
 * @Date 2020-12-07 14:52:29
 * @Description single acl client hook
 **/
public class AclClientHookHolder {

    public static final AclClientHookHolder HOLDER = new AclClientHookHolder();

    private static final AclClientRPCHook EMPTY = new AclClientRPCHook(null);

    private AclClientHookHolder() {

    }

    private AclClientRPCHook hook = null;
    private static final Object LOCK = new Object();

    public AclClientRPCHook getAclHook() {
        if (hook == null) {
            synchronized (LOCK) {
                if (hook == null) {
                    RMQConfigure configure = BeanUtil.getBean(RMQConfigure.class);
                    if (configure.getAccessKey() == null || configure.getAccessKey().trim().isEmpty()) {
                        hook = EMPTY;
                    } else {
                        hook = new AclClientRPCHook(new SessionCredentials(configure.getAccessKey(), configure.getSecretKey()));
                    }
                }
            }
        }
        return hook;
    }

    public boolean isNotEmpty() {
        return hook != EMPTY;
    }
}
