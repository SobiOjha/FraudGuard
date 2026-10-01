package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.model.Integration;

import java.time.Instant;

public class IntegrationResponse {

    private final Long id;
    private final String name;
    private final boolean active;
    private final Instant createdAt;

    private IntegrationResponse(
            Long id,
            String name,
            boolean active,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static IntegrationResponse from(Integration integration) {
        return new IntegrationResponse(
                integration.getId(),
                integration.getName(),
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

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
