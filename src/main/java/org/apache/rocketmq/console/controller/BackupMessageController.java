package org.apache.rocketmq.console.controller;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.console.service.BackupMessageService;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-07-05 16:29:55
 **/
@RestController
@RequestMapping("/backupMessage")
public class BackupMessageController {

    @Autowired
    private BackupMessageService backupMessageService;

    @RequestMapping(value = "/viewMessage.query", method = RequestMethod.GET)
    public Object viewMessage(@RequestParam(required = false) String topic, @RequestParam String msgId) {
        Map<String, Object> messageViewMap = Maps.newHashMap();
        Pair<MessageView, List<MessageTrack>> messageViewListPair = backupMessageService.viewMessage(topic, msgId);
        messageViewMap.put("messageView", messageViewListPair.getObject1());
        messageViewMap.put("messageTrackList", messageViewListPair.getObject2());
        return messageViewMap;
    }

    @RequestMapping(value = "/queryMessageByTopicAndKey.query", method = RequestMethod.GET)
    public Object queryMessageByTopicAndKey(@RequestParam String topic, @RequestParam String key) {
        return backupMessageService.queryMessageByTopicAndKey(topic, key);
    }

    @RequestMapping(value = "/queryMessageByTopic.query", method = RequestMethod.GET)
    public Object queryMessageByTopic(@RequestParam String topic, @RequestParam long begin,
        @RequestParam long end) {
        return backupMessageService.queryMessageByTopic(topic, begin, end);
    }

    @RequestMapping(value = "/queryMessageTotalByTopic.query", method = RequestMethod.GET)
    public Object queryMessageTotalByTopic(@RequestParam String topic, @RequestParam long begin,
        @RequestParam long end) {
        return backupMessageService.queryMessageTotalByTopic(topic, begin, end);
    }
}
