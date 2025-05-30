/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.console.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.rocketmq.console.model.Item;
import org.apache.rocketmq.console.model.Role;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.model.request.UpdateUserRequest;
import org.apache.rocketmq.console.support.JsonResult;

public interface UserService {
    User queryByName(String name);

    User queryByUsernameAndPassword(String username, String password);

    List<Role> selectRoles();

    List<Item> selectItems();

    List<User> selectUserList(User user);

    int addUser(UpdateUserRequest user);

    int updateUser(UpdateUserRequest user);

    int deleteUser(Long id);

    int addRole(Role role);

    int updateRole(Role role);

    int addItem(Item item);

    int updateItem(Item item);

    int updateBelongItem(Map<String, Object> params);

    List<User> selectNotifyUser();

    JsonResult<String> registerAccount(UpdateUserRequest request, String clientFlag);

    Optional<User> selectUserByID(Long id);

    Object forgetPassword(UpdateUserRequest request, String clientFlag);
}
