package org.apache.rocketmq.console.dao;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.dao.ApplySubscriptionGroupDO;
import org.apache.rocketmq.console.model.vo.ApplySubscriptionGroupVO;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-19 19:05:19
 * @description rocketmq-console-ng
 **/
@Mapper
public interface ApplySubscriptionGroupMapper {

    List<ApplySubscriptionGroupDO> selectDOList(ApplySubscriptionGroupDO groupDO);

    int addApplySubscriptionGroup(ApplySubscriptionGroupDO groupDO);

    List<ApplySubscriptionGroupVO> selectVOList(@Param("group") ApplySubscriptionGroupDO groupDO,
        @Param("item") Item item);

    Optional<ApplySubscriptionGroupDO> selectById(Long id);

    int updateApproveSubscriptionGroup(ApplySubscriptionGroupDO groupDO);

    int updateApplyReject(ApplySubscriptionGroupDO groupDO);

    int updateSendStatus(ApplySubscriptionGroupDO groupDO);
}
