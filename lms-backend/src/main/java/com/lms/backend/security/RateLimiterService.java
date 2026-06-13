package com.lms.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    @Value("${app.security.rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${app.security.rate-limit.window-minutes}")
    private int windowMinutes;

    private final Map<String, LoginAttempts> attemptsMap = new ConcurrentHashMap<>();

    public boolean isBlocked(String ipAddress) {
        LoginAttempts attempts = attemptsMap.get(ipAddress);
        if (attempts == null) {
            return false;
        }

        if (attempts.isExpired(windowMinutes)) {
            attemptsMap.remove(ipAddress);
            return false;
        }

        return attempts.getCount() >= maxAttempts;
    }

    public void registerFailedAttempt(String ipAddress) {
        attemptsMap.compute(ipAddress, (key, attempts) -> {
            if (attempts == null || attempts.isExpired(windowMinutes)) {
                return new LoginAttempts(1, LocalDateTime.now());
            }
            attempts.increment();
            return attempts;
        });
    }

    public void resetAttempts(String ipAddress) {
        attemptsMap.remove(ipAddress);
    }

    private static class LoginAttempts {
        private int count;
        private final LocalDateTime firstAttemptTime;

        public LoginAttempts(int count, LocalDateTime firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }

        public int getCount() {
            return count;
        }

        public void increment() {
            this.count++;
        }

        public boolean isExpired(int windowMinutes) {
            return LocalDateTime.now().isAfter(firstAttemptTime.plusMinutes(windowMinutes));
        }
    }
}
