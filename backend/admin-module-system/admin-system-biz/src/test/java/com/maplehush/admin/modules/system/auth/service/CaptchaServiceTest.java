package com.maplehush.admin.modules.system.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maplehush.admin.modules.system.auth.service.impl.CaptchaServiceImpl;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CaptchaServiceTest {

    @Test
    void acceptsLowercaseAnswerAndConsumesChallengeOnce() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        String id = UUID.randomUUID().toString();
        when(values.getAndDelete("auth:captcha:" + id)).thenReturn("A7K2M", null);
        ICaptchaService captchas = new CaptchaServiceImpl(redis, mock(RedisRateLimiter.class));

        captchas.consume(id, "a7k2m");
        assertEquals(
                40001,
                assertThrows(ApiException.class, () -> captchas.consume(id, "A7K2M"))
                        .code());
    }
}
