package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request data required to analyze a transaction for behavioral fraud risk")
public class AnalyzeTransactionRequest {

    @NotBlank(message = "User ID is required")
    @Schema(
            description = "Unique identifier of the user performing the transaction",
            example = "user123"
    )
    private String userId;

    @Positive(message = "Amount must be greater than 0")
    @Schema(
            description = "Transaction amount",
            example = "5000.0"
    )
    private double amount;

    @NotBlank(message = "Currency is required")
    @Schema(
            description = "Currency used for the transaction",
            example = "INR"
    )
    private String currency;

    @NotBlank(message = "Recipient is required")
    @Schema(
            description = "Recipient identifier",
            example = "merchant456"
    )
    private String recipient;

    @NotBlank(message = "Transaction type is required")
    @Schema(
            description = "Type of transaction",
            example = "UPI"
    )
    private String transactionType;

    @NotBlank(message = "Location is required")
    @Schema(
            description = "Location from which the transaction was initiated",
            example = "Delhi"
    )
    private String location;

    @NotBlank(message = "Device ID is required")
    @Schema(
            description = "Identifier of the device used for the transaction",
            example = "device-001"
    )
    private String deviceId;

    @NotNull(message = "Protection mode is required")
    @Schema(
            description = "Fraud protection mode used when deciding the transaction action",
            example = "NORMAL"
    )
    private ProtectionMode protectionMode;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }


    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }


    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }


    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public ProtectionMode getProtectionMode() {
        return protectionMode;
    }

    public void setProtectionMode(ProtectionMode protectionMode) {
        this.protectionMode = protectionMode;
    }
}