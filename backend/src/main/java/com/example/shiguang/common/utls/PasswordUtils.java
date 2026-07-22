package com.example.shiguang.common.utls;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtils {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private PasswordUtils() {
    }

    /**
     * BCrypt 加密（新密码使用）
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * BCrypt 验证
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        // 如果存储的密码以 $2a$ 开头，说明是 BCrypt 格式
        if (encodedPassword != null && encodedPassword.startsWith("$2")) {
            return encoder.matches(rawPassword, encodedPassword);
        }
        // 兼容旧的 MD5 格式
        return md5(rawPassword).equalsIgnoreCase(encodedPassword);
    }

    /**
     * 判断密码是否为 BCrypt 格式
     */
    public static boolean isBcrypt(String encodedPassword) {
        return encodedPassword != null && encodedPassword.startsWith("$2");
    }

    /**
     * MD5 哈希（保留用于兼容旧密码验证）
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}
