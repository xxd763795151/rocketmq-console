package org.apache.rocketmq.console.util;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-10 16:49:47
 * @description rocketmq-console-ng
 **/
public class CommonUtil {

    private static final DateFormat DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String md5(Object o) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(o);
                oos.flush();
                byte[] bytes = baos.toByteArray();
                MessageDigest md5 = MessageDigest.getInstance("md5");

                md5.update(bytes);
                return new String(md5.digest(), "utf-8");
            }
        }

    }

    public static String formatCurrentTime() {
        return DATA_FORMAT.format(new Date());
    }

}
