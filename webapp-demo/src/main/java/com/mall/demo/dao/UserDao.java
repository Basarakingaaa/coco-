package com.mall.demo.dao;

import com.mall.demo.util.DbUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<LoginUser> login(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password, role, status FROM t_user WHERE username = ?";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String passwordHash = resultSet.getString("password");
                int status = resultSet.getInt("status");
                if (status != 1 || !passwordEncoder.matches(password, passwordHash)) {
                    return Optional.empty();
                }
                LoginUser user = new LoginUser(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("role")
                );
                return Optional.of(user);
            }
        }
    }

    public record LoginUser(long id, String username, String role) {
    }
}
