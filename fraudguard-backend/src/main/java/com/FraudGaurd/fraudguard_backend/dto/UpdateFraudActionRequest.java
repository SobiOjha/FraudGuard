package com.FraudGaurd.fraudguard_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request used to update the fraud action of a transaction")
public class UpdateFraudActionRequest {

    @Schema(
            description = "Fraud action to apply to the transaction",
            example = "BLOCK",
            allowableValues = {"APPROVE", "FLAG", "BLOCK"}
    )
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}