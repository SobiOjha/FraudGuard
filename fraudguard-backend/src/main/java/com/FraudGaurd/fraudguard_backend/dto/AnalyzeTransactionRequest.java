package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AnalyzeTransactionRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @Positive(message = "Amount must be greater than 0")
    private double amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    @NotBlank(message = "Transaction type is required")
    private String transactionType;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotNull(message = "Protection mode is required")
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