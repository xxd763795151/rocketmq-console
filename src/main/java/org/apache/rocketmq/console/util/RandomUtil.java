package org.apache.rocketmq.console.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import reactor.core.publisher.Flux;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-10 11:42:54
 * @description rocketmq-console-ng
 **/
public class RandomUtil {


    /**
     * 生成密码字符串
     * 33~47：!~/
     * 48~57：0~9
     * 58~64：:~@
     * 65~90：A~Z
     * 91~96：[~`
     * 97~122：a~z
     * 123~127：{~
     */
    public static List<Character> characters = new ArrayList<>();
    static {
        Flux.range(48, 10).subscribe(i -> characters.add((char)(int)i));
        Flux.range(65, 26).subscribe(i -> characters.add((char)(int)i));
        Flux.range(97, 26).subscribe(i -> characters.add((char)(int)i));
    }

    public static String randomString(int len) {

        int size = characters.size();
        StringBuilder  res = new StringBuilder();
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < len; i++) {
            res.append(characters.get(random.nextInt(size)));
        }

        return res.toString();
    }
}
