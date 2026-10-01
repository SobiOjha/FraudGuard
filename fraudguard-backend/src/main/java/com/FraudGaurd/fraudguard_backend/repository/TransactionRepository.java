package com.FraudGaurd.fraudguard_backend.repository;

import com.FraudGaurd.fraudguard_backend.model.Integration;
import com.FraudGaurd.fraudguard_backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByIntegration(Integration integration);

    Optional<Transaction> findByIdAndIntegration(
            Long id,
            Integration integration
    );

    long countByIntegration(Integration integration);

    long countByIntegrationAndRiskScoreGreaterThanEqual(
            Integration integration,
            int riskScore
    );

    long countByIntegrationAndRiskScoreLessThan(
            Integration integration,
            int riskScore
    );

    long countByIntegrationAndFraudAction(
            Integration integration,
            String fraudAction
    );

    @Query("SELECT AVG(t.riskScore) FROM Transaction t WHERE t.integration = :integration")
    Double findAverageRiskScore(
            @Param("integration") Integration integration
    );

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.integration = :integration
            AND t.userId = :userId
            AND t.analyzedAt >= :since
            """)
    long countRecentTransactions(
            @Param("integration") Integration integration,
            @Param("userId") String userId,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.integration = :integration
            AND t.userId = :userId
            AND t.recipient = :recipient
            AND t.analyzedAt >= :since
            """)
    long countTransactionsToRecipient(
            @Param("integration") Integration integration,
            @Param("userId") String userId,
            @Param("recipient") String recipient,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.integration = :integration
            AND t.userId = :userId
            AND t.recipient = :recipient
            """)
    long countPreviousTransactionsToRecipient(
            @Param("integration") Integration integration,
            @Param("userId") String userId,
            @Param("recipient") String recipient
    );

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.integration = :integration
            AND t.userId = :userId
            AND t.deviceId = :deviceId
            """)
    long countPreviousTransactionsFromDevice(
            @Param("integration") Integration integration,
            @Param("userId") String userId,
            @Param("deviceId") String deviceId
    );
}
