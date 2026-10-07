package com.wangrui.librarymanagement.util;

public class LibrarianPasswordGenerator {
    public static void main(String[] args) {
        String raw = "Admin123!"; // 你想要的管理员登录密码
        String hashed = PasswordUtil.hashPassword(raw);
        System.out.println("Hashed password = " + hashed);
    }
}
// $2a$10$1QxzAiKQvAT0IDo5YUKXyOWKNefjYDS1TCusCihyZHs0ubsMJTS7O