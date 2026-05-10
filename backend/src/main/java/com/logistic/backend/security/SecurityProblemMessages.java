package com.logistic.backend.security;

public final class SecurityProblemMessages {

    private SecurityProblemMessages() {}

    public static String unauthorized(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Требуется авторизация";
        }
        if ("Bad credentials".equalsIgnoreCase(raw) || "Invalid credentials".equalsIgnoreCase(raw)) {
            return "Неверный логин или пароль";
        }
        if ("Full authentication is required to access this resource".equals(raw)) {
            return "Требуется авторизация";
        }
        return raw;
    }

    public static String forbidden(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Доступ запрещён";
        }
        if ("Access Denied".equalsIgnoreCase(raw) || "Access is denied".equalsIgnoreCase(raw)) {
            return "Доступ запрещён";
        }
        return raw;
    }
}
