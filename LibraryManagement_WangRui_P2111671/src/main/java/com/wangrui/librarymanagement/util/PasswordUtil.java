package com.wangrui.librarymanagement.util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.regex.Pattern;

public class PasswordUtil {

    // BCrypt hashing
    public static String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt());
    }

    // Safe verification – if hashed is null/blank, just return false
    public static boolean verifyPassword(String plain, String hashed) {
        if (hashed == null || hashed.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plain, hashed);
        } catch (IllegalArgumentException ex) {
            // Invalid hash format, treat as not matching
            return false;
        }
    }

    // ================= Strong password rule =================

    // At least 8 characters, one upper, one lower, one digit, one special char.
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");

    /**
     * Business rule: check whether a password is "strong" enough to be accepted
     * for system administrators / librarians.
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }
}