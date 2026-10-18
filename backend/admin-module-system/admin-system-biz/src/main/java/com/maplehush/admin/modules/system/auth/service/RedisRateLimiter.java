package com.maplehush.admin.modules.system.auth.service;

import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String key, int maxRequests, Duration window) {
        Long count = redis.execute(INCREMENT_WITH_EXPIRY, List.of(key), Long.toString(window.toSeconds()));
        if (count == null || count > maxRequests) {
            throw ApiException.rateLimited();
        }
    }

    public void reset(String key) {
        redis.delete(key);
    }
}
