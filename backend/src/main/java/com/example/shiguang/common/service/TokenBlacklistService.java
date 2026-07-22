package com.example.shiguang.common.service;

import com.example.shiguang.common.utls.JwtUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(value = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class TokenBlacklistService {
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToBlacklist(String token) {
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) return;
        long remainingMs = JwtUtils.getRemainingMs(token);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + userId + ":" + token, "1", remainingMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + userId + ":" + token));
    }
}
