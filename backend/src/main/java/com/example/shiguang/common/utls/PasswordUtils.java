package com.example.shiguang.common.utls;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public final class PasswordUtils {
    private PasswordUtils() {
    }

    public static String md5(String plainText) {
        return DigestUtils.md5DigestAsHex(plainText.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String plainText, String encryptedPassword) {
        return md5(plainText).equalsIgnoreCase(encryptedPassword);
    }
}
