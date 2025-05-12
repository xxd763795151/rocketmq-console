package org.apache.rocketmq.console.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.console.dao.SubItemInfoMapper;
import org.apache.rocketmq.console.model.dao.SubItemInfoDO;
import org.apache.rocketmq.console.model.request.SubItemInfoRequest;
import org.apache.rocketmq.console.model.vo.SubItemInfoVO;
import org.apache.rocketmq.console.service.SubItemInfoService;
import org.apache.rocketmq.console.support.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 19:50:43
 * @description rocketmq-console-ng
 **/
@Service
@Slf4j
public class SubItemInfoServiceImpl implements SubItemInfoService {

    @Autowired
    private SubItemInfoMapper subItemInfoMapper;

    @Override public Object add(SubItemInfoRequest request) {

        SubItemInfoDO params = new SubItemInfoDO();
        params.setSubItemCode(request.getSubItemCode());
        List<SubItemInfoDO> subItemInfoDOS = subItemInfoMapper.selectSubItemInfoDOList(params);
        if (CollectionUtils.isNotEmpty(subItemInfoDOS)) {
            return JsonResult.failed("该项目编码已被其它项目使用");
        }
        subItemInfoMapper.add(request.convertTo(SubItemInfoDO.class));
        return JsonResult.success();
    }

    @Override public Object delete(SubItemInfoRequest request) {
        subItemInfoMapper.delete(request.convertTo(SubItemInfoDO.class));
        return JsonResult.success();
    }

    @Override public Object update(@RequestBody SubItemInfoRequest request) {
        subItemInfoMapper.update(request.convertTo(SubItemInfoDO.class));
        return JsonResult.success();
    }

    @Override public Object select(SubItemInfoRequest request) {
        PageHelper.startPage(request.getPage(), request.getLimit());
        List<SubItemInfoVO> subItemInfoVOList = subItemInfoMapper.selectSubItemInfoVOList(request.convertTo(SubItemInfoDO.class));
        return new JsonResult<>(new PageInfo<>(subItemInfoVOList));
    }

    @Override public Object selectGroupByItem(SubItemInfoRequest request) {
        return new JsonResult<>(subItemInfoMapper.selectGroupByItem(request.convertTo(SubItemInfoDO.class)));
    }
}
