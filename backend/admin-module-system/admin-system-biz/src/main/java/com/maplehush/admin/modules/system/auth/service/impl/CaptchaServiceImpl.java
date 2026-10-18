package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.modules.system.auth.service.ICaptchaService;
import com.maplehush.admin.modules.system.auth.service.RedisRateLimiter;
import com.maplehush.admin.modules.system.auth.vo.CaptchaChallengeVO;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CaptchaServiceImpl implements ICaptchaService {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Duration CAPTCHA_TTL = Duration.ofSeconds(120);
    private static final Duration CAPTCHA_RATE_WINDOW = Duration.ofMinutes(1);

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;
    private final RedisRateLimiter rateLimiter;

    public CaptchaServiceImpl(StringRedisTemplate redis, RedisRateLimiter rateLimiter) {
        this.redis = redis;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public CaptchaChallengeVO issue(String ipAddress) {
        rateLimiter.check("auth:captcha:ip:" + ipAddress, 20, CAPTCHA_RATE_WINDOW);
        StringBuilder answer = new StringBuilder(5);
        for (int index = 0; index < 5; index++) {
            answer.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        String id = UUID.randomUUID().toString();
        redis.opsForValue().set("auth:captcha:" + id, answer.toString(), CAPTCHA_TTL);
        return new CaptchaChallengeVO(id, drawImage(answer.toString()), CAPTCHA_TTL.toSeconds());
    }

    @Override
    public void consume(String id, String response) {
        try {
            UUID.fromString(id);
        } catch (RuntimeException exception) {
            throw ApiException.captchaInvalid();
        }
        String answer = redis.opsForValue().getAndDelete("auth:captcha:" + id);
        if (answer == null
                || response == null
                || !MessageDigest.isEqual(
                        answer.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                        response.toUpperCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            throw ApiException.captchaInvalid();
        }
    }

    private String drawImage(String answer) {
        BufferedImage image = new BufferedImage(160, 52, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(246, 248, 252));
            graphics.fillRect(0, 0, 160, 52);
            graphics.setStroke(new BasicStroke(1.2f));
            for (int line = 0; line < 7; line++) {
                graphics.setColor(
                        new Color(150 + random.nextInt(80), 150 + random.nextInt(80), 150 + random.nextInt(80)));
                graphics.drawLine(random.nextInt(160), random.nextInt(52), random.nextInt(160), random.nextInt(52));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            for (int index = 0; index < answer.length(); index++) {
                graphics.setColor(new Color(30 + random.nextInt(70), 40 + random.nextInt(80), 70 + random.nextInt(80)));
                graphics.drawString(String.valueOf(answer.charAt(index)), 12 + index * 29, 38);
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bytes);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not render captcha", exception);
        }
    }
}
