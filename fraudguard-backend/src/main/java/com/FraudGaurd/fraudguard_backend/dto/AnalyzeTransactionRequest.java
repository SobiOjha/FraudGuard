package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;

public class AnalyzeTransactionRequest {

    private String userId;

    private double amount;

    private String currency;

    private String recipient;

    private String transactionType;

    private String location;

    private String deviceId;

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