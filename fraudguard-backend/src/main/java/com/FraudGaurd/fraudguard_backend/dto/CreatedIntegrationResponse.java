package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.model.Integration;

import java.time.Instant;

public class CreatedIntegrationResponse {

    private final Long id;
    private final String name;
    private final String apiKey;
    private final boolean active;
    private final Instant createdAt;

    private CreatedIntegrationResponse(
            Long id,
            String name,
            String apiKey,
            boolean active,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.apiKey = apiKey;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static CreatedIntegrationResponse from(
            Integration integration,
            String apiKey) {
        return new CreatedIntegrationResponse(
                integration.getId(),
                integration.getName(),
                apiKey,
                integration.isActive(),
                integration.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
