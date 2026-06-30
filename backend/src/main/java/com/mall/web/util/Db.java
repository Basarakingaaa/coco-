package com.mall.web.util;

import java.io.InputStream;
import java.sql.*;
import java.util.*;

public final class Db {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream inputStream = Db.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                PROPS.load(inputStream);
            }
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private Db() {
    }

    public static Connection conn() throws SQLException {
        return DriverManager.getConnection(config("DB_URL", "db.url"), config("DB_USERNAME", "db.username"), config("DB_PASSWORD", "db.password"));
    }

    public static List<Map<String, Object>> list(String sql, Object... args) throws SQLException {
        try (Connection connection = conn(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= meta.getColumnCount(); index++) {
                        row.put(meta.getColumnLabel(index), rs.getObject(index));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public static Map<String, Object> one(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> rows = list(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static long insert(String sql, Object... args) throws SQLException {
        try (Connection connection = conn(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, args);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        }
    }

    public static int update(String sql, Object... args) throws SQLException {
        try (Connection connection = conn(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            return statement.executeUpdate();
        }
    }

    public static void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int index = 0; index < args.length; index++) {
            statement.setObject(index + 1, args[index]);
        }
    }

    private static String config(String envName, String propName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envName);
        }
        if (value == null || value.isBlank()) {
            value = PROPS.getProperty(propName, "");
        }
        return value;
    }
}
