package org.apache.rocketmq.console.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.rocketmq.console.model.BelongItem;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.dao.BelongItemDO;
import org.apache.rocketmq.console.model.dao.ItemDO;

/**
 * @Author xuxd
 * @Date 2020-11-27 10:07:56
 * @Description belong_item
 **/
@Mapper
public interface BelongItemMapper {

    @Insert("<script> insert into belong_item(type, topic_consumer_name, item_id) VALUES " +
        "<foreach item='item' collection='params.item' open='(' close=')' index='i' separator='),('>" +
        "#{params.type}, #{params.name}, #{item} " +
        "</foreach></script>")
    int addBatchSingleType(@Param("params") Map<String, Object> params);

    @Delete("delete from belong_item where topic_consumer_name = #{name}")
    int deleteBy(Map<String, Object> params);

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "topic_consumer_name", property = "name"),
        @Result(column = "type", property = "type"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_code", property = "item.code")})
    @Select("select b.id, b.topic_consumer_name, b.type, i.item_id, i.item_name, i.code as item_code from belong_item b join item i on b.item_id = i.id where type = #{type}")
    List<BelongItem> selectBelongItem(BelongItem item);

    // type = 1
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "topic_consumer_name", property = "name"),
        @Result(column = "type", property = "type"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_code", property = "item.code")})
    @Select("<script> " +
        "select b.id, b.topic_consumer_name, b.type, b.item_id, i.item_name, i.code as item_code " +
        "from belong_item b join item i on b.item_id = i.id " +
        "where type = 1 and b.topic_consumer_name in " +
        "<foreach item='item' index='i' open='(' close=')' separator=',' collection='list'>#{item}</foreach>" +
        "</script>")
    List<BelongItem> selectTopicItem(List<String> topic);

    // type = 2
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "topic_consumer_name", property = "name"),
        @Result(column = "type", property = "type"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_code", property = "item.code")})
    @Select("<script> " +
        "select b.id, b.topic_consumer_name, b.type, b.item_id, i.item_name, i.code as item_code " +
        "from belong_item b join item i on b.item_id = i.id " +
        "where type = 2 and b.topic_consumer_name in " +
        "<foreach item='item' index='i' open='(' close=')' separator=',' collection='list'>#{item}</foreach>" +
        "</script>")
    List<BelongItem> selectSubscriptionItem(List<String> group);

    // type = 1: topic, 2: subscription
    @Select("select b.topic_consumer_name from belong_item b join item i on i.id = b.item_id join userinfo u on i.id = u.item_id where b.type = 1 and u.username =#{username}")
    List<String> selectTopicByUser(@Param("username") String username);

    @Select("select b.topic_consumer_name from belong_item b join item i on i.id = b.item_id join userinfo u on i.id = u.item_id where b.type = 2 and u.username =#{username}")
    List<String> selectSubscriptionByUser(@Param("username") String username);

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "topic_consumer_name", property = "name"),
        @Result(column = "type", property = "type"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_code", property = "item.code")})
    @Select("<script> " +
        "select b.id, b.topic_consumer_name, b.type, b.item_id, i.item_name, i.code as item_code " +
        "from belong_item b join item i on b.item_id = i.id " +
        "where b.item_id in " +
        "<foreach item='item' index='i' open='(' close=')' separator=',' collection='list'>#{item.id}</foreach>" +
        "</script>")
    List<BelongItem> selectByItemList(List<Item> items);

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "topic_consumer_name", property = "name"),
        @Result(column = "type", property = "type"),
        @Result(column = "item_id", property = "item.id")})
    @Select("select b.id, b.topic_consumer_name, b.type, b.item_id from belong_item b " +
        "where type = #{type} and item_id = #{itemId} and topic_consumer_name = #{name} and type = #{type}")
    List<BelongItem> selectByItem(ItemDO item);

    @Insert("insert into belong_item(type, topic_consumer_name, item_id) " +
        "select #{type}, #{name}, #{itemId} from dual " +
        "where not exists(select id from belong_item where type = #{type} and item_id = #{itemId} and topic_consumer_name = #{name} and type = #{type})")
    int addIfNotExist(BelongItemDO belongItem);
}
