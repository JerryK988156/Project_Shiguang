package com.example.shiguang.service;

import com.example.shiguang.common.utls.PasswordUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Test
    void testPasswordEncodeAndMatch() {
        String rawPassword = "123456";
        String encoded = PasswordUtils.encode(rawPassword);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2"));
        assertTrue(PasswordUtils.matches(rawPassword, encoded));
    }

    @Test
    void testPasswordMismatch() {
        String encoded = PasswordUtils.encode("123456");
        assertFalse(PasswordUtils.matches("wrong", encoded));
    }

    @Test
    void testMd5Compatibility() {
        String md5Hash = PasswordUtils.md5("123456");
        assertEquals(32, md5Hash.length());
        // MD5 hash of "123456" should match
        assertTrue(PasswordUtils.matches("123456", md5Hash));
    }

    @Test
    void testBcryptDetection() {
        String bcryptHash = PasswordUtils.encode("test");
        assertTrue(PasswordUtils.isBcrypt(bcryptHash));

        String md5Hash = PasswordUtils.md5("test");
        assertFalse(PasswordUtils.isBcrypt(md5Hash));
    }
}
