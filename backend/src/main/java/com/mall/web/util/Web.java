package com.mall.web.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Web {
    public static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private Web() {
    }

    public static Map<String, Object> body(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Map<?, ?> raw = GSON.fromJson(json, Map.class);
        Map<String, Object> body = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> body.put(String.valueOf(key), value));
        }
        return body;
    }

    public static void ok(HttpServletResponse response, Object data) throws IOException {
        write(response, 200, Map.of("code", 200, "message", "OK", "data", data == null ? "" : data));
    }

    public static void fail(HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, Map.of("code", status, "message", message));
    }

    public static void write(HttpServletResponse response, int status, Object payload) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(payload));
    }

    public static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public static int intValue(Object value, int defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    public static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
