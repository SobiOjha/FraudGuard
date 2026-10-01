package com.FraudGaurd.fraudguard_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials required to authenticate a FraudGuard user")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Registered username", example = "himanshu")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "SecurePassword123")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}