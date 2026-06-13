package com.lms.backend.service.impl;

import com.lms.backend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String RESEND_API_KEY = "re_Pox9vUoq_JXU47dVW2J37wqWevdWW7ShC";
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void sendOtp(String to, String otp) {
        log.info("Sending OTP email to {}", to);
        try {
            String payload = String.format(
                "{\"from\":\"LIMS Security <noreply@programmingprophet.site>\",\"to\":[\"%s\"],\"subject\":\"Your LIMS Password Reset OTP\",\"html\":\"<p>Your OTP code is <strong>%s</strong>. It is valid for 10 minutes.</p>\"}",
                to, otp
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + RESEND_API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("OTP email successfully sent to {}", to);
            } else {
                log.error("Failed to send OTP via Resend. Status code: {}, Response: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to send verification email. Resend status code: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error sending OTP email: ", e);
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }
}
