package com.FraudGaurd.fraudguard_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private double amount;

    private String currency;

    private String recipient;

    private String transactionType;

    private String location;

    private String deviceId;

    private boolean isNewRecipient;

    private boolean isNewDevice;

    private int recentTransactionCount;

    private int transactionsInTimeWindow;

    private int repeatedTransactionsToRecipient;

    private int riskScore;

    private String riskLevel;

    private String fraudAction;

    @ElementCollection
    private List<String> reasons;

    private LocalDateTime analyzedAt;

    public Long getId() {
        return id;
    }

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
        return isNewRecipient;
    }

    public void setNewRecipient(boolean newRecipient) {
        isNewRecipient = newRecipient;
    }

    public boolean isNewDevice() {
        return isNewDevice;
    }

    public void setNewDevice(boolean newDevice) {
        isNewDevice = newDevice;
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

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getFraudAction() {
        return fraudAction;
    }

    public void setFraudAction(String fraudAction) {
        this.fraudAction = fraudAction;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}