package org.apache.rocketmq.console.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.rocketmq.console.model.Role;

import java.util.List;

/**
 * @Author xuxd
 * @Date 2020-11-24 22:06:34
 * @Description role dao
 **/
@Mapper
public interface RoleMapper {

    @Select("select id, role_name as name, type from role")
    List<Role> selectAll();

    @Insert("insert into role(role_name, type) values (#{name}, #{type})")
    int addRole(Role role);

    @Update("update role set role_name = #{name}, type = #{type} where id = #{id}")
    int updateRole(Role role);

    @Select("select id, role_name as name, type from role where type = #{type}")
    List<Role> selectByType(@Param("type") int type);
}
