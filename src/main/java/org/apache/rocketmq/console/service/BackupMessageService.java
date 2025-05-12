package org.apache.rocketmq.console.service;

import java.util.List;
import java.util.Map;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.tools.admin.api.MessageTrack;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-07-05 15:09:01
 **/
public interface BackupMessageService {

    Map<String, Object> registerReput(Map<String, String> params);

    /**
     * @param subject
     * @param msgId
     */
    Pair<MessageView, List<MessageTrack>> viewMessage(String subject, final String msgId);

    List<MessageView> queryMessageByTopicAndKey(final String topic, final String key);

    /**
     * @param topic
     * @param begin
     * @param end
     * org.apache.rocketmq.tools.command.message.PrintMessageSubCommand
     */
    List<MessageView> queryMessageByTopic(final String topic, final long begin,
        final long end);

    /**
     * @param topic
     * @param begin
     * @param end
     * org.apache.rocketmq.tools.command.message.PrintMessageSubCommand
     */
    Object queryMessageTotalByTopic(final String topic, final long begin,
        final long end);

    List<MessageTrack> messageTrackDetail(MessageExt msg);

}
