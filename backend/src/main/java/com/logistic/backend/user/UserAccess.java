package com.logistic.backend.user;

public final class UserAccess {

    private UserAccess() {}

    public static boolean isAdmin(User user) {
        return user != null && user.getRoles().contains(Role.ADMIN);
    }
}
