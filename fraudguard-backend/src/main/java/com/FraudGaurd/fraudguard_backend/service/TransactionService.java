package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionRequest;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import com.FraudGaurd.fraudguard_backend.dto.DashboardStatsResponse;
import com.FraudGaurd.fraudguard_backend.model.Transaction;
import com.FraudGaurd.fraudguard_backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FraudAnalysisService fraudAnalysisService;
    private final FraudProtectionService fraudProtectionService;

    public TransactionService(
            TransactionRepository transactionRepository,
            FraudAnalysisService fraudAnalysisService,
            FraudProtectionService fraudProtectionService) {

        this.transactionRepository = transactionRepository;
        this.fraudAnalysisService = fraudAnalysisService;
        this.fraudProtectionService = fraudProtectionService;
    }

    public AnalyzeTransactionResponse analyzeTransaction(
            AnalyzeTransactionRequest request) {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime last24Hours = now.minusHours(24);
        LocalDateTime last10Minutes = now.minusMinutes(10);

        long recentTransactionCount =
                transactionRepository.countRecentTransactions(
                        request.getUserId(),
                        last24Hours
                );

        long transactionsInTimeWindow =
                transactionRepository.countRecentTransactions(
                        request.getUserId(),
                        last10Minutes
                );

        long repeatedTransactionsToRecipient =
                transactionRepository.countTransactionsToRecipient(
                        request.getUserId(),
                        request.getRecipient(),
                        last24Hours
                );

        boolean isNewRecipient =
                transactionRepository.countPreviousTransactionsToRecipient(
                        request.getUserId(),
                        request.getRecipient()
                ) == 0;

        boolean isNewDevice =
                transactionRepository.countPreviousTransactionsFromDevice(
                        request.getUserId(),
                        request.getDeviceId()
                ) == 0;

        AnalyzeTransactionResponse analysis =
                fraudAnalysisService.analyze(
                        request,
                        (int) recentTransactionCount,
                        (int) transactionsInTimeWindow,
                        (int) repeatedTransactionsToRecipient,
                        isNewRecipient,
                        isNewDevice
                );

        FraudAction action =
                fraudProtectionService.decideAction(
                        analysis.getRiskScore(),
                        request.getProtectionMode()
                );

        Transaction transaction = new Transaction();

        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setRecipient(request.getRecipient());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setLocation(request.getLocation());
        transaction.setDeviceId(request.getDeviceId());

        transaction.setNewRecipient(isNewRecipient);
        transaction.setNewDevice(isNewDevice);

        transaction.setRecentTransactionCount(
                (int) recentTransactionCount
        );

        transaction.setTransactionsInTimeWindow(
                (int) transactionsInTimeWindow
        );

        transaction.setRepeatedTransactionsToRecipient(
                (int) repeatedTransactionsToRecipient
        );

        transaction.setRiskScore(
                analysis.getRiskScore()
        );

        transaction.setRiskLevel(
                analysis.getRiskLevel()
        );

        transaction.setFraudAction(
                action.name()
        );

        transaction.setReasons(
                analysis.getReasons()
        );

        transaction.setAnalyzedAt(now);

        transactionRepository.save(transaction);

        analysis.setTransactionId(transaction.getId());
        analysis.setAction(action);

        return analysis;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public DashboardStatsResponse getDashboardStats() {

        long totalTransactions =
                transactionRepository.count();

        long highRiskTransactions =
                transactionRepository
                        .countByRiskScoreGreaterThanEqual(60);

        long safeTransactions =
                transactionRepository
                        .countByRiskScoreLessThan(30);

        long blockedTransactions =
                transactionRepository
                        .countByFraudAction("BLOCK");

        Double averageRiskScore =
                transactionRepository.findAverageRiskScore();

        if (averageRiskScore == null) {
            averageRiskScore = 0.0;
        }

        return new DashboardStatsResponse(
                totalTransactions,
                highRiskTransactions,
                averageRiskScore,
                safeTransactions,
                blockedTransactions
        );
    }

    public Transaction updateFraudAction(
            Long transactionId,
            String action) {

        Transaction transaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Transaction not found with ID: "
                                                + transactionId
                                )
                        );

        if (!action.equals("APPROVE")
                && !action.equals("FLAG")
                && !action.equals("BLOCK")) {

            throw new IllegalArgumentException(
                    "Invalid fraud action: " + action
            );
        }

        transaction.setFraudAction(action);

        return transactionRepository.save(transaction);
    }
}