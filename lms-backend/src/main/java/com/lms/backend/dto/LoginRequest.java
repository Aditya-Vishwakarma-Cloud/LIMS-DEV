package com.lms.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/*
 * Accepts login input from client

Keeps entity safe from exposure

Allows validation
 * LOGIN REQUEST DTO
 * -----------------
 * Used to accept login credentials from client.
 */

@Data
public class LoginRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
