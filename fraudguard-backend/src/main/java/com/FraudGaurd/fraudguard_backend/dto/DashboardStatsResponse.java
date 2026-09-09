package com.FraudGaurd.fraudguard_backend.dto;

public class DashboardStatsResponse {

    private long totalTransactions;
    private long highRiskTransactions;
    private Double averageRiskScore;
    private long safeTransactions;
    private long blockedTransactions;

    public DashboardStatsResponse(
            long totalTransactions,
            long highRiskTransactions,
            Double averageRiskScore,
            long safeTransactions,
            long blockedTransactions) {

        this.totalTransactions = totalTransactions;
        this.highRiskTransactions = highRiskTransactions;
        this.averageRiskScore = averageRiskScore;
        this.safeTransactions = safeTransactions;
        this.blockedTransactions = blockedTransactions;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public long getHighRiskTransactions() {
        return highRiskTransactions;
    }

    public Double getAverageRiskScore() {
        return averageRiskScore;
    }

    public long getSafeTransactions() {
        return safeTransactions;
    }

    public long getBlockedTransactions() {
        return blockedTransactions;
    }
}