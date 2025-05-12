package org.apache.rocketmq.console.model.request;

import javax.validation.constraints.NotBlank;

/**
 * @Author xuxd
 * @Date 2020-11-23 17:22:17
 * @Description add /update user bean. Does not exist DO BO DTO，it is meaningless in the current project.
 **/
public class UpdateUserRequest {

    private Long id;
    @NotBlank(message = "name is null")
    private String name;
    @NotBlank(message = "password is null")
    private String password;
    private Long role;
    private Long item;
    private int notify;//1: true, 0: false
    private String email = "";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Long getRole() {
        return role;
    }

    public void setRole(Long role) {
        this.role = role;
    }

    public Long getItem() {
        return item;
    }

    public void setItem(Long item) {
        this.item = item;
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
        return "UpdateUserRequest{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", password='" + password + '\'' +
            ", role=" + role +
            ", item=" + item +
            ", notify=" + notify +
            ", email='" + email + '\'' +
            '}';
    }
}
