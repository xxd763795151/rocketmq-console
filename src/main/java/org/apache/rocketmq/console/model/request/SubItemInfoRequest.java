package org.apache.rocketmq.console.model.request;

import lombok.Data;
import org.apache.rocketmq.console.model.dao.SubItemInfoDO;

/**
 * rocketmq-console-ng.
 *
 * @author xuxd
 * @date 2021-04-15 19:49:20
 * @description rocketmq-console-ng
 **/
@Data
public class SubItemInfoRequest {

    private Long id;

    private String subItemCode;

    private String subItemName;

    private String description;

    private Long itemId;

    private int page;

    private int limit;

    private String username;

    public SubItemInfoDO convertTo(Class<? extends SubItemInfoDO> cls) {
        SubItemInfoDO subItemInfoDO = new SubItemInfoDO();
        subItemInfoDO.setDescription(this.description);
        subItemInfoDO.setItemId(this.itemId);
        subItemInfoDO.setSubItemCode(this.subItemCode);
        subItemInfoDO.setSubItemName(this.subItemName);
        subItemInfoDO.setUsername(this.username);
        subItemInfoDO.setId(this.id);
        return subItemInfoDO;
    }
}
