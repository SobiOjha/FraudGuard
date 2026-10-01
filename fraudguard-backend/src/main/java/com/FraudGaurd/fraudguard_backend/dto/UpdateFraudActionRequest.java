package com.FraudGaurd.fraudguard_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request used to update the fraud action of a transaction")
public class UpdateFraudActionRequest {

    @Schema(
            description = "Fraud action to apply to the transaction",
            example = "BLOCK",
            allowableValues = {"APPROVE", "FLAG", "BLOCK"}
    )
    @NotBlank(message = "Fraud action is required")
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
