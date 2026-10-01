package com.FraudGaurd.fraudguard_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateIntegrationRequest {

    @NotBlank(message = "Integration name is required")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
