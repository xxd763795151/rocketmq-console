package org.apache.rocketmq.console.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.rocketmq.console.model.Item;

import java.util.List;

/**
 * @Author xuxd
 * @Date 2020-11-25 10:48:32
 * @Description item dao interface
 **/
@Mapper
public interface ItemMapper {

    @Select("select id, item_name as name, code from item")
    List<Item> selectAll();

    @Insert("insert into item (item_name, code) values (#{name}, #{code})")
    int addItem(Item item);

    @Update("update item set item_name = #{name}, code = #{code} where id = #{id}")
    int updateItem(Item item);
}
