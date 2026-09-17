package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionRequest;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FraudAnalysisService {

    public AnalyzeTransactionResponse analyze(
            AnalyzeTransactionRequest request,
            int recentTransactionCount,
            int transactionsInTimeWindow,
            int repeatedTransactionsToRecipient,
            boolean isNewRecipient,
            boolean isNewDevice) {

        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        // 1. Transaction amount
        if (request.getAmount() >= 50000) {

            riskScore += 20;
            reasons.add("Unusually high transaction amount");
        }

        // 2. New recipient
        if (isNewRecipient) {

            riskScore += 15;
            reasons.add("Transaction is being made to a new recipient");
        }

        // 3. New device
        if (isNewDevice) {

            riskScore += 20;
            reasons.add("Transaction originated from a new device");
        }

        // 4. Location anomaly
        if (request.getLocation() != null
                && !request.getLocation().equalsIgnoreCase("Delhi")) {

            riskScore += 20;
            reasons.add("Transaction location is unusual");
        }

        // 5. Recent transaction frequency
        if (recentTransactionCount >= 10) {

            riskScore += 15;
            reasons.add("High recent transaction frequency");

        } else if (recentTransactionCount >= 5) {

            riskScore += 10;
            reasons.add("Elevated recent transaction frequency");
        }

        // 6. Transaction burst within 10 minutes
        if (transactionsInTimeWindow >= 5) {

            riskScore += 15;
            reasons.add("Unusually high transaction activity within a short time");

        } else if (transactionsInTimeWindow >= 3) {

            riskScore += 10;
            reasons.add("Elevated transaction activity within a short time");
        }

        // 7. Repeated transactions to same recipient
        if (repeatedTransactionsToRecipient >= 5) {

            riskScore += 10;
            reasons.add("Repeated transactions to the same recipient");

        } else if (repeatedTransactionsToRecipient >= 3) {

            riskScore += 5;
            reasons.add("Multiple recent transactions to the same recipient");
        }

        // Keep score between 0 and 100
        riskScore = Math.min(riskScore, 100);

        // Risk classification
        String riskLevel;

        if (riskScore >= 80) {

            riskLevel = "CRITICAL";

        } else if (riskScore >= 60) {

            riskLevel = "HIGH";

        } else if (riskScore >= 30) {

            riskLevel = "SUSPICIOUS";

        } else {

            riskLevel = "SAFE";
        }

        // Default action.
        // ProtectionMode will be applied by FraudProtectionService.
        FraudAction action = FraudAction.APPROVE;

        return new AnalyzeTransactionResponse(
                riskScore,
                riskLevel,
                reasons,
                action
        );
    }
}