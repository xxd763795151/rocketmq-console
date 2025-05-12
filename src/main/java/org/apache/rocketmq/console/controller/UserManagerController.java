package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.console.interceptor.security.NotAdminDisable;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.Role;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.UpdateUserRequest;
import org.apache.rocketmq.console.service.UserService;
import org.apache.rocketmq.console.support.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author xuxd
 * @Date 2020-11-23 10:39:30
 * @Description User Info manage
 **/
@RestController
@RequestMapping("/user")
@Validated
@NotAdminDisable
public class UserManagerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagerController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/list.query")
    public List<User> getUserInfoList() {
        return userService.selectUserList(new User());
    }

    @GetMapping("/role/list.query")
    public List<Role> getRoleInfoList() {
        return userService.selectRoles();
    }

    @GetMapping("/item/list.query")
    public List<Item> getItemList() {
        return userService.selectItems();
    }

    @GetMapping("/info/all.query")
    public Object getUserInfo() {
        Map<String, Object> res = new HashMap<>();
        res.put("role", userService.selectRoles());
        res.put("item", userService.selectItems());
        return res;
    }

    @PostMapping("/add.do")
    public Object saveUser(@Validated @RequestBody UpdateUserRequest user) {
        LOGGER.info("Add new user: {}", user);
        userService.addUser(user);
        return new JsonResult(0, "");
    }

    @PostMapping("/update.do")
    public Object updateUser(@Validated @RequestBody UpdateUserRequest user) {
        Objects.requireNonNull(user.getId(), "ID is null");
        LOGGER.info("update: {}", user);
        userService.updateUser(user);
        return new JsonResult(0, "");
    }

    @DeleteMapping("/delete/{id}")
    public Object deleteUser(@PathVariable Long id) {
        LOGGER.info("delete user, {}", id);
        Objects.requireNonNull(id, "ID is null");
        userService.deleteUser(id);
        return new JsonResult(0, "");
    }

    @PostMapping("/role/add.do")
    public Object addRole(@RequestBody Role role) {
        Objects.requireNonNull(role.getName(), "Name is null");
        userService.addRole(role);
        return new JsonResult(0, "");
    }

    @PostMapping("/role/update.do")
    public Object updateRole(@RequestBody Role role) {
        Objects.requireNonNull(role.getName(), "Name is null");
        Objects.requireNonNull(role.getId(), "Role ID is null");
        userService.updateRole(role);
        return new JsonResult(0, "");
    }

    @PostMapping("/item/add.do")
    public Object addItem(@RequestBody Item item) {
        Objects.requireNonNull(item.getName(), "Name is null");
        userService.addItem(item);
        return new JsonResult(0, "");
    }

    @PostMapping("/item/update.do")
    public Object updateItem(@RequestBody Item item) {
        Objects.requireNonNull(item.getName(), "Name is null");
        Objects.requireNonNull(item.getId(), "Item ID is null");
        userService.updateItem(item);
        return new JsonResult(0, "");
    }
// InetAddress.getLocalHost().getHostName() took 5005 milliseconds to respond. Please verify your network configuration (macOS machines may need to add entries to /etc/hosts).
    @PostMapping("/item/belong/update.do")
    public Object updateBelongItem(@RequestBody Map<String, Object> params) {
        LOGGER.info("param: {}", params);
        Objects.requireNonNull(params.get("type"), "Type is null");
        Objects.requireNonNull(params.get("name"), "Name is null");
        userService.updateBelongItem(params);
        return new JsonResult(0, "");
    }
}
