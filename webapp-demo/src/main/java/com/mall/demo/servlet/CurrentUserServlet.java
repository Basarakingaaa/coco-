package com.mall.demo.servlet;

import com.mall.demo.dao.UserDao.LoginUser;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/current-user")
public class CurrentUserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Object value = request.getSession(false) == null ? null : request.getSession(false).getAttribute("loginUser");
        if (!(value instanceof LoginUser user)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"未登录\"}");
            return;
        }

        response.getWriter().write(String.format(
                "{\"success\":true,\"user\":{\"id\":%d,\"username\":\"%s\",\"role\":\"%s\"}}",
                user.id(),
                escapeJson(user.username()),
                escapeJson(user.role())
        ));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
