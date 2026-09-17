package com.FraudGaurd.fraudguard_backend.repository;

import com.FraudGaurd.fraudguard_backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    long countByRiskScoreGreaterThanEqual(int riskScore);

    long countByRiskScoreLessThan(int riskScore);

    long countByFraudAction(String fraudAction);

    @Query("SELECT AVG(t.riskScore) FROM Transaction t")
    Double findAverageRiskScore();

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.userId = :userId
            AND t.analyzedAt >= :since
            """)
    long countRecentTransactions(
            @Param("userId") String userId,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.userId = :userId
            AND t.recipient = :recipient
            AND t.analyzedAt >= :since
            """)
    long countTransactionsToRecipient(
            @Param("userId") String userId,
            @Param("recipient") String recipient,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.userId = :userId
            AND t.recipient = :recipient
            """)
    long countPreviousTransactionsToRecipient(
            @Param("userId") String userId,
            @Param("recipient") String recipient
    );

    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.userId = :userId
            AND t.deviceId = :deviceId
            """)
    long countPreviousTransactionsFromDevice(
            @Param("userId") String userId,
            @Param("deviceId") String deviceId
    );
}