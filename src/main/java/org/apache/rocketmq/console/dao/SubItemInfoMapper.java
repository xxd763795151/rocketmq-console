package org.apache.rocketmq.console.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.rocketmq.console.model.dao.SubItemInfoDO;
import org.apache.rocketmq.console.model.vo.SubItemInfoVO;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 19:31:05
 * @description rocketmq-console-ng
 **/
@Mapper
public interface SubItemInfoMapper {

    void add(SubItemInfoDO subItemInfoDO);

    List<SubItemInfoVO> selectSubItemInfoVOList(SubItemInfoDO subItemInfoDO);

    void update(SubItemInfoDO subItemInfoDO);

    void delete(SubItemInfoDO subItemInfoDO);

    List<SubItemInfoDO> selectSubItemInfoDOList(SubItemInfoDO subItemInfoDO);

    List<SubItemInfoDO> selectGroupByItem(SubItemInfoDO subItemInfoDO);
}
