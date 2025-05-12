package org.apache.rocketmq.console.service;

import org.apache.rocketmq.console.model.request.SubItemInfoRequest;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 19:48:21
 * @description rocketmq-console-ng
 **/
public interface SubItemInfoService {

    Object add(SubItemInfoRequest request);

    Object delete(SubItemInfoRequest request);

    Object update(SubItemInfoRequest request);

    Object select(SubItemInfoRequest request);

    Object selectGroupByItem(SubItemInfoRequest request);
}
