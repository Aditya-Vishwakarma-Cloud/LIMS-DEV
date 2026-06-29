package com.lms.backend.service;

public interface EmailService {
    void sendOtp(String to, String otp);
    void sendEmail(String to, String subject, String htmlContent);
}
