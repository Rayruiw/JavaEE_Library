package com.wangrui.librarymanagement.util;

public class TestHash {
    public static void main(String[] args) {
        String plain = "Admin123!";
        String hash = PasswordUtil.hashPassword(plain);
        System.out.println(hash);
    }
}
//$2a$10$WYeZOU4mHYIwKzJxx/GWdeAwwpwXpDNO0PpfbBQT6GTadp128pR8y
//账号：admin@example.com   密码：Admin123!
