package com.mall.service.impl;

import com.mall.common.BusinessException;
import com.mall.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final String CODE_PREFIX = "register:code:";
    private static final String COOLDOWN_PREFIX = "register:code:cooldown:";
    private static final long COOLDOWN_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${mall.verify-code.expire-seconds}")
    private long expireSeconds;

    @Value("${mall.verify-code.sender}")
    private String sender;

    public VerificationCodeServiceImpl(RedisTemplate<String, Object> redisTemplate, JavaMailSender mailSender) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
    }

    @Override
    public void sendRegisterCode(String email) {
        String cooldownKey = COOLDOWN_PREFIX + email;
        Boolean firstRequest = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));
        if (Boolean.FALSE.equals(firstRequest)) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("注册验证码");
        message.setText("您的注册验证码是：" + code + "，" + (expireSeconds / 60) + " 分钟内有效。");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            redisTemplate.delete(cooldownKey);
            throw new BusinessException(500, "验证码邮件发送失败，请稍后重试");
        }

        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public void verifyRegisterCode(String email, String code) {
        String key = CODE_PREFIX + email;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null || !cached.toString().equals(code)) {
            throw new BusinessException("验证码错误或已过期");
        }
        redisTemplate.delete(key);
    }
}
