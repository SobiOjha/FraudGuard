package com.FraudGaurd.fraudguard_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregated fraud analysis statistics used by the FraudGuard dashboard")
public class DashboardStatsResponse {

    @Schema(
            description = "Total number of analyzed transactions",
            example = "150"
    )
    private long totalTransactions;

    @Schema(
            description = "Number of transactions classified as high risk",
            example = "32"
    )
    private long highRiskTransactions;

    @Schema(
            description = "Average risk score across analyzed transactions",
            example = "42.75"
    )
    private Double averageRiskScore;

    @Schema(
            description = "Number of transactions classified as safe",
            example = "98"
    )
    private long safeTransactions;

    @Schema(
            description = "Number of transactions that were blocked by the fraud protection system",
            example = "20"
    )
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