package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidFraudActionException;
import com.FraudGaurd.fraudguard_backend.ExceptionHandler.TransactionNotFoundException;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionRequest;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import com.FraudGaurd.fraudguard_backend.dto.DashboardStatsResponse;
import com.FraudGaurd.fraudguard_backend.model.Integration;
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
    private final AuthenticatedIntegrationService authenticatedIntegrationService;

    public TransactionService(
            TransactionRepository transactionRepository,
            FraudAnalysisService fraudAnalysisService,
            FraudProtectionService fraudProtectionService,
            AuthenticatedIntegrationService authenticatedIntegrationService) {

        this.transactionRepository = transactionRepository;
        this.fraudAnalysisService = fraudAnalysisService;
        this.fraudProtectionService = fraudProtectionService;
        this.authenticatedIntegrationService = authenticatedIntegrationService;
    }

    public AnalyzeTransactionResponse analyzeTransaction(
            AnalyzeTransactionRequest request) {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime last24Hours = now.minusHours(24);
        LocalDateTime last10Minutes = now.minusMinutes(10);
        Integration integration =
                authenticatedIntegrationService.getRequiredIntegration();

        long recentTransactionCount =
                transactionRepository.countRecentTransactions(
                        integration,
                        request.getUserId(),
                        last24Hours
                );

        long transactionsInTimeWindow =
                transactionRepository.countRecentTransactions(
                        integration,
                        request.getUserId(),
                        last10Minutes
                );

        long repeatedTransactionsToRecipient =
                transactionRepository.countTransactionsToRecipient(
                        integration,
                        request.getUserId(),
                        request.getRecipient(),
                        last24Hours
                );

        boolean isNewRecipient =
                transactionRepository.countPreviousTransactionsToRecipient(
                        integration,
                        request.getUserId(),
                        request.getRecipient()
                ) == 0;

        boolean isNewDevice =
                transactionRepository.countPreviousTransactionsFromDevice(
                        integration,
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

        transaction.setIntegration(integration);
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
        return transactionRepository.findAllByIntegration(
                authenticatedIntegrationService.getRequiredIntegration()
        );
    }

    public DashboardStatsResponse getDashboardStats() {
        Integration integration =
                authenticatedIntegrationService.getRequiredIntegration();

        long totalTransactions =
                transactionRepository.countByIntegration(integration);

        long highRiskTransactions =
                transactionRepository
                        .countByIntegrationAndRiskScoreGreaterThanEqual(
                                integration,
                                60
                        );

        long safeTransactions =
                transactionRepository
                        .countByIntegrationAndRiskScoreLessThan(
                                integration,
                                30
                        );

        long blockedTransactions =
                transactionRepository.countByIntegrationAndFraudAction(
                        integration,
                        "BLOCK"
                );

        Double averageRiskScore =
                transactionRepository.findAverageRiskScore(integration);

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
                transactionRepository.findByIdAndIntegration(
                        transactionId,
                        authenticatedIntegrationService.getRequiredIntegration()
                )
                        .orElseThrow(() ->
                                new TransactionNotFoundException(
                                        "Transaction not found with ID: "
                                                + transactionId
                                )
                        );

        if (!action.equals("APPROVE")
                && !action.equals("FLAG")
                && !action.equals("BLOCK")) {

            throw new InvalidFraudActionException(
                    "Invalid fraud action: " + action
            );
        }

        transaction.setFraudAction(action);

        return transactionRepository.save(transaction);
    }
}
