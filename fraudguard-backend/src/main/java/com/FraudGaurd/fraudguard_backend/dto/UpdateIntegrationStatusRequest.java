package com.FraudGaurd.fraudguard_backend.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateIntegrationStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
