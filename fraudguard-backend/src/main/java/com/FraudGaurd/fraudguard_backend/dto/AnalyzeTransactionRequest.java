package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalyzeTransactionRequest {

    private String userId;

    private double amount;

    private String currency;

    private String recipient;

    private String transactionType;

    private String location;

    private String deviceId;

    @JsonProperty("isNewRecipient")
    private boolean newRecipient;

    @JsonProperty("isNewDevice")
    private boolean newDevice;

    private int recentTransactionCount;

    private int transactionsInTimeWindow;

    private int repeatedTransactionsToRecipient;

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


    public boolean isNewRecipient() {
        return newRecipient;
    }

    public void setNewRecipient(boolean newRecipient) {
        this.newRecipient = newRecipient;
    }


    public boolean isNewDevice() {
        return newDevice;
    }

    public void setNewDevice(boolean newDevice) {
        this.newDevice = newDevice;
    }


    public int getRecentTransactionCount() {
        return recentTransactionCount;
    }

    public void setRecentTransactionCount(int recentTransactionCount) {
        this.recentTransactionCount = recentTransactionCount;
    }


    public int getTransactionsInTimeWindow() {
        return transactionsInTimeWindow;
    }

    public void setTransactionsInTimeWindow(int transactionsInTimeWindow) {
        this.transactionsInTimeWindow = transactionsInTimeWindow;
    }


    public int getRepeatedTransactionsToRecipient() {
        return repeatedTransactionsToRecipient;
    }

    public void setRepeatedTransactionsToRecipient(
            int repeatedTransactionsToRecipient) {

        this.repeatedTransactionsToRecipient =
                repeatedTransactionsToRecipient;
    }


    public ProtectionMode getProtectionMode() {
        return protectionMode;
    }

    public void setProtectionMode(ProtectionMode protectionMode) {
        this.protectionMode = protectionMode;
    }
}