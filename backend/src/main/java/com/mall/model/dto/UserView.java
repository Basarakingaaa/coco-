package com.mall.model.dto;

import com.mall.model.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserView {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;

    public static UserView from(User user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setPhone(user.getPhone());
        view.setEmail(user.getEmail());
        view.setRole(user.getRole());
        view.setStatus(user.getStatus());
        view.setCreatedAt(user.getCreatedAt());
        return view;
    }
}
