package org.apache.rocketmq.console.common;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.apache.rocketmq.console.support.CommonExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author xuxd
 * @Date 4:22 下午 2020/11/18
 * @Description SendMessageAdapter
 **/
@Component
public class SendMessageAdapter {

    private static final ExecutorService SERVICE = CommonExecutors.EXECUTOR_SERVICE;

    @Autowired
    private SendMailClient sendMailClient;

    @Value("${mail.username}")
    private String sendFrom;

    public void send(final String to, final Map<String, Object> content, final String type) {
        switch (type) {
//            case "email":
            default:
                SERVICE.submit(new EmailSendTask(sendFrom, to, content, sendMailClient));
        }
    }

    class EmailSendTask implements Runnable {

        String from;
        String to;
        Map<String, Object> content;
        SendMailClient sendMailClient;

        public EmailSendTask(String from, String to, Map<String, Object> content, SendMailClient sendMailClient) {
            this.from = from;
            this.to = to;
            this.content = content;
            this.sendMailClient = sendMailClient;
        }

        @Override
        public void run() {
            Arrays.stream(to.split(",")).forEach(t -> {
                sendMailClient.sendHtml(from, t, "RocketMQ昨日消息统计", "notify", content);
            });
        }
    }
}
