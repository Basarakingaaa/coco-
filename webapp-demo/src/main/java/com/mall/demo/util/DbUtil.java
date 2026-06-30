package com.mall.demo.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DbUtil {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = DbUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
            }
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private DbUtil() {
    }

    public static Connection getConnection() throws SQLException {
        String url = readConfig("DB_URL", "db.url");
        String username = readConfig("DB_USERNAME", "db.username");
        String password = readConfig("DB_PASSWORD", "db.password");
        return DriverManager.getConnection(url, username, password);
    }

    private static String readConfig(String envName, String propertyName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getProperty(envName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return PROPERTIES.getProperty(propertyName, "");
    }
}
