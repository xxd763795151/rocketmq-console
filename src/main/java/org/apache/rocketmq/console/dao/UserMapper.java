package org.apache.rocketmq.console.dao;

import java.util.Optional;
import org.apache.ibatis.annotations.*;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.UpdateUserRequest;

import java.util.List;

/**
 * @Author xuxd
 * @Date 2020-11-25 11:07:14
 * @Description userinfo
 **/
@Mapper
public interface UserMapper {
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "username", property = "name"),
            @Result(column = "password", property = "password"),
            @Result(column = "notify", property = "notify"),
            @Result(column = "email", property = "email"),
            @Result(column = "role_id", property = "role.id"),
            @Result(column = "role_name", property = "role.name"),
            @Result(column = "role_type", property = "role.type"),
            @Result(column = "item_name", property = "item.name"),
            @Result(column = "item_id", property = "item.id"),
            @Result(column = "item_code", property = "item.code")})
    @Select("select u.id, u.username, u.password, u.notify, u.email, u.role_id, u.item_id, r.role_name, r.type as role_type, i.id as item_id, i.item_name, i.code as item_code " +
        "from userinfo u join role r on r.id = u.role_id join item i on i.id = u.item_id")
    List<User> selectUserList(User user);

//    @Insert("insert into userinfo(username, password, role_id, item_id, notify, email) values(#{name}, #{password}, #{role}, #{item}, #{notify}, #{email})")
    int addUser(UpdateUserRequest user);

    @Update("update userinfo set username=#{name}, password=#{password}, role_id=#{role}, item_id=#{item}, notify=#{notify}, email=#{email} where id = #{id}")
    int updateUser(UpdateUserRequest user);

    @Delete("delete from userinfo where id = #{id}")
    int deleteUser(@Param("id") Long id);

    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "username", property = "name"),
            @Result(column = "password", property = "password"),
            @Result(column = "role_id", property = "role.id"),
            @Result(column = "role_name", property = "role.name"),
            @Result(column = "role_type", property = "role.type"),
            @Result(column = "item_name", property = "item.name"),
            @Result(column = "item_id", property = "item.id"),
            @Result(column = "item_code", property = "item.code")})
    @Select("select u.id, u.username, u.password, u.role_id, u.item_id, r.role_name, r.type as role_type, i.id as item_id, i.item_name, i.code as item_code from " +
            "userinfo u join role r on r.id = u.role_id join item i on i.id = u.item_id where u.username=#{username} and u.password=#{password}")
    User selectByNameAndPass(@Param("username") String username, @Param("password") String password);

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "name"),
        @Result(column = "password", property = "password"),
        @Result(column = "notify", property = "notify"),
        @Result(column = "email", property = "email"),
        @Result(column = "role_id", property = "role.id"),
        @Result(column = "role_name", property = "role.name"),
        @Result(column = "role_type", property = "role.type"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_code", property = "item.code")})
    @Select("select u.id, u.username, u.password, u.notify, u.email, u.role_id, u.item_id, r.role_name, r.type as role_type, i.id as item_id, i.item_name, i.code as item_code " +
        "from userinfo u join role r on r.id = u.role_id join item i on i.id = u.item_id where u.notify != 0 and u.email is not null and length(trim(u.email)) > 0")
    List<User> selectNotifyUser();

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "name"),
        @Result(column = "password", property = "password"),
        @Result(column = "notify", property = "notify"),
        @Result(column = "email", property = "email"),
        @Result(column = "role_id", property = "role.id"),
        @Result(column = "role_name", property = "role.name"),
        @Result(column = "role_type", property = "role.type"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_code", property = "item.code")})
    @Select("select u.id, u.username, u.password, u.notify, u.email, u.role_id, u.item_id, r.role_name, r.type as role_type, i.id as item_id, i.item_name, i.code as item_code " +
        "from userinfo u join role r on r.id = u.role_id join item i on i.id = u.item_id where u.username = #{name}")
    Optional<User> selectUserByName(@Param("name") String name);

    @Select("select u.id, u.username as name, u.password, u.notify, u.email, u.role_id, u.item_id from userinfo u where u.email = #{email}")
    Optional<User> selectUserByEmail(@Param("email") String email);

    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "name"),
        @Result(column = "password", property = "password"),
        @Result(column = "email", property = "email"),
        @Result(column = "role_id", property = "role.id"),
        @Result(column = "role_name", property = "role.name"),
        @Result(column = "role_type", property = "role.type"),
        @Result(column = "item_name", property = "item.name"),
        @Result(column = "item_id", property = "item.id"),
        @Result(column = "item_code", property = "item.code")})
    @Select("select u.id, u.username, u.password, u.role_id, u.item_id, u.email, r.role_name, r.type as role_type, i.id as item_id, i.item_name, i.code as item_code from " +
        "userinfo u join role r on r.id = u.role_id join item i on i.id = u.item_id where u.id=#{id}")
    Optional<User> selectUserByID(@Param("id") Long id);
}
