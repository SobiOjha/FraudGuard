package com.FraudGaurd.fraudguard_backend.repository;

import com.FraudGaurd.fraudguard_backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    long countByRiskScoreGreaterThanEqual(int riskScore);

    long countByRiskScoreLessThan(int riskScore);

    long countByFraudAction(String fraudAction);

    @Query("SELECT AVG(t.riskScore) FROM Transaction t")
    Double findAverageRiskScore();
}