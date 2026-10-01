package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.model.User;
import com.FraudGaurd.fraudguard_backend.model.UserRole;

public class AuthResponse {

    private final Long id;
    private final String username;
    private final UserRole role;

    private AuthResponse(Long id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public static AuthResponse from(User user) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }
}
