package com.lms.backend.auth;

import com.lms.backend.dto.AuthResponse;
import com.lms.backend.dto.ChangePasswordRequest;
import com.lms.backend.dto.RequestOtpRequest;
import com.lms.backend.dto.LoginRequest;
import com.lms.backend.dto.UserRegisterRequest;
import com.lms.backend.dto.UserResponse;
import com.lms.backend.service.EmailService;
import java.util.concurrent.ConcurrentHashMap;
import com.lms.backend.entity.AccountStatus;
import com.lms.backend.entity.Role;
import com.lms.backend.entity.User;
import com.lms.backend.entity.RefreshToken;
import com.lms.backend.exception.EmailAlreadyExistsException;
import com.lms.backend.exception.InvalidCredentialsException;
import com.lms.backend.exception.RateLimitException;
import com.lms.backend.exception.TokenRefreshException;
import com.lms.backend.repository.RoleRepository;
import com.lms.backend.repository.UserRepository;
import com.lms.backend.repository.RefreshTokenRepository;
import com.lms.backend.security.JwtService;
import com.lms.backend.security.RateLimiterService;
import com.lms.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    private final ConcurrentHashMap<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    private static class OtpData {
        String code;
        LocalDateTime expiry;

        OtpData(String code, LocalDateTime expiry) {
            this.code = code;
            this.expiry = expiry;
        }
    }

    @Value("${app.security.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public UserResponse register(UserRegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            auditLogService.log("REGISTRATION_FAILED", request.getEmail(), ipAddress, "Email already exists");
            throw new EmailAlreadyExistsException("Email is already registered: " + request.getEmail());
        }

        Role role = null;
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String roleNameStr = request.getRoles().iterator().next();
            role = roleRepository.findByName(roleNameStr).orElse(null);
        }
        if (role == null) {
            role = roleRepository.findByName("ROLE_CLIENT_VIEWER").orElse(null);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(AccountStatus.ACTIVE)
                .deleted(false)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        auditLogService.log("USER_REGISTERED", savedUser.getEmail(), ipAddress, "Role assigned: " + 
                (role != null ? role.getName() : "None"));

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, HttpServletResponse response) {
        if (rateLimiterService.isBlocked(ipAddress)) {
            auditLogService.log("LOGIN_BLOCKED", request.getEmail(), ipAddress, "Too many login attempts");
            throw new RateLimitException("Too many failed login attempts. Please try again after 15 minutes.");
        }

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            rateLimiterService.registerFailedAttempt(ipAddress);
            auditLogService.log("LOGIN_FAILED", request.getEmail(), ipAddress, "Invalid credentials");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userOpt.get();

        if (user.getStatus() != AccountStatus.ACTIVE) {
            auditLogService.log("LOGIN_FAILED", user.getEmail(), ipAddress, "Account is " + user.getStatus());
            throw new InvalidCredentialsException("Account is not active. Status: " + user.getStatus());
        }

        rateLimiterService.resetAttempts(ipAddress);

        // Revoke any existing refresh tokens for this user first (Stateless Single-Session rule or rotation cleanup)
        refreshTokenRepository.deleteByUser(user);

        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiryDate(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        
        // HTTP-Only Cookie settings
        setRefreshTokenCookie(response, refreshTokenStr, refreshExpirationMs);

        auditLogService.log("LOGIN_SUCCESS", user.getEmail(), ipAddress, "Successful login");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenStr, String ipAddress, HttpServletResponse response) {
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new TokenRefreshException("N/A", "Refresh token is missing");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new TokenRefreshException(refreshTokenStr, "Refresh token not found"));

        if (refreshToken.isRevoked()) {
            // Compromise detection: Token has been used twice or compromised
            // Revoke all tokens for this user
            refreshTokenRepository.deleteByUser(refreshToken.getUser());
            auditLogService.log("COMPROMISE_DETECTED", refreshToken.getUser().getEmail(), ipAddress, 
                    "Attempted reuse of revoked refresh token. Revoked all tokens for user.");
            throw new TokenRefreshException(refreshTokenStr, "Refresh token has been revoked (Compromise Detected)");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException(refreshTokenStr, "Refresh token is expired");
        }

        User user = refreshToken.getUser();
        
        // Revoke the old token (rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new Access and Refresh tokens
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshTokenStr = UUID.randomUUID().toString();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenStr)
                .user(user)
                .expiryDate(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        setRefreshTokenCookie(response, newRefreshTokenStr, refreshExpirationMs);
        auditLogService.log("TOKEN_REFRESH", user.getEmail(), ipAddress, "Access token rotated successfully");

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr, String ipAddress, HttpServletResponse response) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                auditLogService.log("LOGOUT", token.getUser().getEmail(), ipAddress, "Successful logout");
            });
        }
        
        // Clear HTTP Only Cookie
        setRefreshTokenCookie(response, "", 0);
    }

    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        return mapToUserResponse(user);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeMillis) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/") // allow Next.js middleware to read it across all routes
                .maxAge(maxAgeMillis / 1000)
                .sameSite("Lax") // Lax works better for cross-port localhost redirects
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Transactional
    public void requestOtp(String email, RequestOtpRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect current password");
        }

        String code = String.format("%06d", new java.util.Random().nextInt(1000000));
        otpStorage.put(email, new OtpData(code, LocalDateTime.now().plusMinutes(10)));

        emailService.sendOtp(email, code);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect current password");
        }

        OtpData otpData = otpStorage.get(email);
        if (otpData == null) {
            throw new InvalidCredentialsException("OTP verification required. Please request a code.");
        }

        if (otpData.expiry.isBefore(LocalDateTime.now())) {
            otpStorage.remove(email);
            throw new InvalidCredentialsException("OTP has expired. Please request a new one.");
        }

        if (!otpData.code.equals(request.getOtp())) {
            throw new InvalidCredentialsException("Invalid OTP code.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStorage.remove(email);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .roles(user.getRole() != null ? java.util.Set.of(user.getRole().getName()) : java.util.Collections.emptySet())
                .build();
    }
}
