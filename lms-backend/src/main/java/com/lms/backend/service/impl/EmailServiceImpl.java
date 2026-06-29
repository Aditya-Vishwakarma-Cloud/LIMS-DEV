package com.lms.backend.service.impl;

import com.lms.backend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.email.resend-api-key}")
    private String resendApiKey;

    @Value("${app.email.from-domain}")
    private String fromDomain;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void sendOtp(String to, String otp) {
        // log.info("Sending OTP email to {}", to);
        log.info("Sending OTP email to {} with OTP: {}", to, otp); // Log OTP for easy testing
        try {
            String payload = String.format(
                    "{\"from\":\"LIMS Security <noreply@%s>\",\"to\":[\"%s\"],\"subject\":\"Your LIMS Password Reset OTP\",\"html\":\"<p>Your OTP code is <strong>%s</strong>. It is valid for 10 minutes.</p>\"}",
                    fromDomain, to, otp);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("OTP email successfully sent to {}", to);
            } else {
                log.error("Failed to send OTP via Resend. Status code: {}, Response: {}", response.statusCode(),
                        response.body());
                throw new RuntimeException(
                        "Failed to send verification email. Resend status code: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error sending OTP email: ", e);
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendEmail(String to, String subject, String htmlContent) {
        log.info("Sending email to {}: {}", to, subject);
        try {
            String escapedHtml = htmlContent.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            String payload = String.format(
                    "{\"from\":\"LIMS Notification <noreply@%s>\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    fromDomain, to, subject.replace("\"", "\\\""), escapedHtml);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Notification email successfully sent to {}", to);
            } else {
                log.error("Failed to send email via Resend. Status code: {}, Response: {}", response.statusCode(),
                        response.body());
            }
        } catch (Exception e) {
            log.error("Error sending notification email: ", e);
        }
    }
}
