package com.mall.service;

public interface VerificationCodeService {
    void sendRegisterCode(String email);
    void verifyRegisterCode(String email, String code);
}
