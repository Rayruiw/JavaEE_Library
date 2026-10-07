package com.wangrui.librarymanagement.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    // ================= existing tests =================

    @Test
    public void testHashAndVerify_Success() {
        String raw = "Abc12345!";
        String hashed = PasswordUtil.hashPassword(raw);

        // 1. hashed password should not equal the raw password
        assertNotEquals(raw, hashed, "Hashed password should not equal raw password");

        // 2. correct password should pass verification
        assertTrue(
                PasswordUtil.verifyPassword(raw, hashed),
                "Correct password should pass verification"
        );
    }

    @Test
    public void testHashAndVerify_FailOnWrongPassword() {
        String raw = "Abc12345!";
        String hashed = PasswordUtil.hashPassword(raw);

        // wrong password must NOT pass verification
        assertFalse(
                PasswordUtil.verifyPassword("WrongPassword123!", hashed),
                "Wrong password should NOT pass verification"
        );
    }

    @Test
    public void testHashDifferentForSamePassword() {
        String raw = "Abc12345!";

        String hashed1 = PasswordUtil.hashPassword(raw);
        String hashed2 = PasswordUtil.hashPassword(raw);

        // Because of salt, two hashes should be different
        assertNotEquals(
                hashed1, hashed2,
                "Hash for the same password is expected to be different due to salt"
        );
    }

    // ================= new business-rule tests =================

    @Test
    public void testStrongPasswordRule_AcceptStrongPassword() {
        // length >= 8, contains upper, lower, digit, special char
        String pwd = "Abc12345!";

        assertTrue(
                PasswordUtil.isStrongPassword(pwd),
                "Valid strong password should be accepted"
        );
    }

    @Test
    public void testStrongPasswordRule_RejectWeakPasswords() {
        // too short
        assertFalse(PasswordUtil.isStrongPassword("Abc1!"),
                "Too short password should be rejected");

        // no uppercase
        assertFalse(PasswordUtil.isStrongPassword("abc12345!"),
                "Password without uppercase letter should be rejected");

        // no lowercase
        assertFalse(PasswordUtil.isStrongPassword("ABC12345!"),
                "Password without lowercase letter should be rejected");

        // no digit
        assertFalse(PasswordUtil.isStrongPassword("Abcdefg!"),
                "Password without digit should be rejected");

        // no special character
        assertFalse(PasswordUtil.isStrongPassword("Abc123456"),
                "Password without special character should be rejected");

        // null
        assertFalse(PasswordUtil.isStrongPassword(null),
                "Null password should be rejected");
    }
}