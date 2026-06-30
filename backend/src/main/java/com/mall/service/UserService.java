package com.mall.service;

import com.mall.model.User;
import com.mall.model.dto.LoginRequest;
import com.mall.model.dto.RegisterRequest;
import org.springframework.data.domain.Page;

public interface UserService {
    String register(RegisterRequest request);
    String login(LoginRequest request);

    // ---- 管理端 ----
    Page<User> adminListUsers(int page, int size);
    User adminUpdateStatus(Long userId, Integer status);
}
