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
package org.apache.rocketmq.console.model;

import org.hibernate.validator.constraints.Range;

public class User {
    public static final int ORDINARY = 0;
    public static final int ADMIN = 1;
    public static final int SEND = 2;
    public static final int ALL_DATA = 3;

    private long id;
    private String name;
    private String password;
    @Range(min = 0, max = 10)
    private int type = 0;
    private Role role;
    private Item item;
    private int notify;//1: true, 0: false
    private String email;

    public User() {
    }

    public User(String name, String password, int type) {
        this.name = name;
        this.password = password;
        this.type = type;
    }

    public User cloneOne() {
        return new User(this.name, this.password, this.type);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public boolean isAdmin() {
        return this.type == ADMIN;
    }

    public boolean isAllData() {
        return this.type == ADMIN || this.type == ALL_DATA;
    }

    public int getNotify() {
        return notify;
    }

    public void setNotify(int notify) {
        this.notify = notify;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override public String toString() {
        return "User{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", password='" + password + '\'' +
            ", type=" + type +
            ", role=" + role +
            ", item=" + item +
            ", notify=" + notify +
            ", email='" + email + '\'' +
            '}';
    }
}
