package com.mall.common;

import com.mall.security.JwtAuthFilter.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserHolder {

    private CurrentUserHolder() {
    }

    public static Long getUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user.userId();
        }
        throw new BusinessException(401, "未登录");
    }
}
