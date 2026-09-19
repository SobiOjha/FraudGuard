package com.FraudGaurd.fraudguard_backend.dto;

import com.FraudGaurd.fraudguard_backend.service.FraudAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of FraudGuard's behavioral fraud analysis")
public class AnalyzeTransactionResponse {

    @Schema(
            description = "Unique identifier assigned to the analyzed transaction",
            example = "42"
    )
    private Long transactionId;

    @Schema(
            description = "Calculated behavioral fraud risk score from 0 to 100",
            example = "65"
    )
    private int riskScore;

    @Schema(
            description = "Risk classification based on the calculated risk score",
            example = "HIGH"
    )
    private String riskLevel;

    @Schema(
            description = "Reasons explaining why the transaction received its risk score",
            example = "[\"New device detected\", \"High transaction frequency\"]"
    )
    private List<String> reasons;

    @Schema(
            description = "Protection action determined for the transaction",
            example = "FLAG"
    )
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