package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.service.FraudAction;

import java.util.List;

public class AnalyzeTransactionResponse {

    private Long transactionId;
    private int riskScore;
    private String riskLevel;
    private List<String> reasons;
    private FraudAction action;

    public AnalyzeTransactionResponse(
            int riskScore,
            String riskLevel,
            List<String> reasons,
            FraudAction action) {

        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.reasons = reasons;
        this.action = action;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public FraudAction getAction() {
        return action;
    }

    public void setAction(FraudAction action) {
        this.action = action;
    }
}