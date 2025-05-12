package org.apache.rocketmq.console.dao;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.dao.ApplyTopicDO;
import org.apache.rocketmq.console.model.vo.ApplyTopicVO;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 10:59:19
 * @description rocketmq-console-ng
 **/
@Mapper
public interface ApplyTopicMapper {

    List<ApplyTopicDO> selectApplyTopicDOList(ApplyTopicDO applyTopicDO);

    int addApplyTopic(ApplyTopicDO applyTopicDO);

    List<ApplyTopicVO> selectApplyTopicVOList(@Param("topic") ApplyTopicDO applyTopicDO, @Param("item") Item item);

    Optional<ApplyTopicDO> selectById(Long id);

    int updateApproveTopic(ApplyTopicDO applyTopicDO);

    int updateApplyReject(ApplyTopicDO applyTopicDO);

    int updateSendStatus(ApplyTopicDO applyTopicDO);
}
