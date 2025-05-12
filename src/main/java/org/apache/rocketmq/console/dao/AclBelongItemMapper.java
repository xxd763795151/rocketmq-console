package org.apache.rocketmq.console.dao;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.rocketmq.console.model.AclBelongInfo;

/**
 * @Author xuxd
 * @Date 2020-12-17 17:41:59
 * @Description rocketmq-console-ng
 **/
@Mapper
public interface AclBelongItemMapper {

    @Insert("<script>" +
        "insert into acl_belong_item(access_key, item_id) values " +
        "<foreach item='item' index='index' collection='itemIdList' open=' (' close=') ' separator=' ),( '> " +
        "#{accessKey}, #{item}" +
        "</foreach>" +
        "</script>")
    int insertAclBelongInfo(AclBelongInfo info);

    @Delete("delete from acl_belong_item where access_key = #{accessKey}")
    int deleteBelongInfoByAccessKey(String accessKey);

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "access_key", property = "accessKey"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_code", property = "item.code")})
    @Select("<script> select abi.id, abi.access_key, i.id as item_id, i.item_name, i.code as item_code from acl_belong_item abi join item i on abi.item_id = i.id where abi.access_key in " +
        "<foreach item='accessKey' index='i' collection='list' open=' ( ' close=' ) ' separator=','> " +
        "#{accessKey}" +
        "</foreach>" +
        "</script>")
    List<AclBelongInfo> selectAclBelongInfosByAccessKey(List<String> accessKeyList);

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "access_key", property = "accessKey"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_code", property = "item.code")})
    @Select("select abi.id, abi.access_key, i.id as item_id, i.item_name, i.code as item_code " +
        "from acl_belong_item abi join item i on abi.item_id = i.id " +
        "where abi.item_id = #{itemId}")
    List<AclBelongInfo> selectAclBelongInfoByItem(Long itemId);
}
