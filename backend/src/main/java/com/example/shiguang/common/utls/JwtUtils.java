package com.example.shiguang.common.utls;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JwtUtils {
    private static final String SECRET = "shiguang-plan-jwt-secret-key-2026";
    private static final long ACCESS_EXPIRATION = 7200000L; // 2小时
    private static final long REFRESH_EXPIRATION = 604800000L; // 7天

    private JwtUtils() {
    }

    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成访问令牌，有效期 2 小时 */
    public static String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ACCESS_EXPIRATION);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    /** 生成刷新令牌，有效期 7 天 */
    public static String generateRefreshToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + REFRESH_EXPIRATION);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    public static Long getUserId(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return null;
        }
        return Long.parseLong(claims.getSubject());
    }

    public static String getUserRole(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return null;
        }
        return claims.get("role", String.class);
    }

    public static boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 获取 token 剩余有效时间（毫秒） */
    public static long getRemainingMs(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /** 校验刷新令牌 */
    public static boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /** 从已过期的令牌中提取 userId（可接受任意 token 字符串） */
    public static Long getUserIdFromExpiredToken(String token) {
        try {
            return getUserId(token);
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    private static Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
