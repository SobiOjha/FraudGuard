package com.FraudGaurd.fraudguard_backend.repository;

import com.FraudGaurd.fraudguard_backend.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("dev")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;


    @Test
    void shouldCountRecentTransactions() {

        LocalDateTime now = LocalDateTime.now();

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusHours(2),
                        40,
                        "FLAG"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-02",
                        "DEVICE-01",
                        now.minusHours(30),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1002",
                        "RECIPIENT-03",
                        "DEVICE-02",
                        now.minusHours(1),
                        60,
                        "BLOCK"
                )
        );

        long count =
                transactionRepository.countRecentTransactions(
                        "U1001",
                        now.minusHours(24)
                );

        assertEquals(1, count);
    }


    @Test
    void shouldCountTransactionsWithinTimeWindow() {

        LocalDateTime now = LocalDateTime.now();

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusMinutes(5),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-02",
                        "DEVICE-01",
                        now.minusMinutes(8),
                        30,
                        "FLAG"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-03",
                        "DEVICE-01",
                        now.minusMinutes(20),
                        40,
                        "FLAG"
                )
        );

        long count =
                transactionRepository.countRecentTransactions(
                        "U1001",
                        now.minusMinutes(10)
                );

        assertEquals(2, count);
    }


    @Test
    void shouldCountRecentTransactionsToRecipient() {

        LocalDateTime now = LocalDateTime.now();

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusHours(1),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusHours(2),
                        30,
                        "FLAG"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-02",
                        "DEVICE-01",
                        now.minusHours(1),
                        40,
                        "FLAG"
                )
        );

        long count =
                transactionRepository.countTransactionsToRecipient(
                        "U1001",
                        "RECIPIENT-01",
                        now.minusHours(24)
                );

        assertEquals(2, count);
    }


    @Test
    void shouldCountPreviousTransactionsToRecipient() {

        LocalDateTime now = LocalDateTime.now();

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusDays(5),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusDays(2),
                        30,
                        "FLAG"
                )
        );

        long count =
                transactionRepository
                        .countPreviousTransactionsToRecipient(
                                "U1001",
                                "RECIPIENT-01"
                        );

        assertEquals(2, count);
    }


    @Test
    void shouldCountPreviousTransactionsFromDevice() {

        LocalDateTime now = LocalDateTime.now();

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusDays(2),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-02",
                        "DEVICE-02",
                        now.minusDays(1),
                        30,
                        "FLAG"
                )
        );

        long count =
                transactionRepository
                        .countPreviousTransactionsFromDevice(
                                "U1001",
                                "DEVICE-01"
                        );

        assertEquals(1, count);
    }


    @Test
    void shouldReturnCorrectDashboardCounts() {

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        LocalDateTime.now(),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-02",
                        "DEVICE-01",
                        LocalDateTime.now(),
                        70,
                        "BLOCK"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1002",
                        "RECIPIENT-03",
                        "DEVICE-02",
                        LocalDateTime.now(),
                        90,
                        "BLOCK"
                )
        );

        assertEquals(
                2,
                transactionRepository
                        .countByRiskScoreGreaterThanEqual(60)
        );

        assertEquals(
                1,
                transactionRepository
                        .countByRiskScoreLessThan(30)
        );

        assertEquals(
                2,
                transactionRepository
                        .countByFraudAction("BLOCK")
        );
    }


    @Test
    void shouldCalculateAverageRiskScore() {

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        LocalDateTime.now(),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1002",
                        "RECIPIENT-02",
                        "DEVICE-02",
                        LocalDateTime.now(),
                        60,
                        "BLOCK"
                )
        );

        Double average =
                transactionRepository.findAverageRiskScore();

        assertEquals(40.0, average);
    }


    private Transaction createTransaction(
            String userId,
            String recipient,
            String deviceId,
            LocalDateTime analyzedAt,
            int riskScore,
            String fraudAction) {

        Transaction transaction = new Transaction();

        transaction.setUserId(userId);
        transaction.setAmount(1000);
        transaction.setCurrency("INR");
        transaction.setRecipient(recipient);
        transaction.setTransactionType("TRANSFER");
        transaction.setLocation("Delhi");
        transaction.setDeviceId(deviceId);

        transaction.setNewRecipient(false);
        transaction.setNewDevice(false);

        transaction.setRecentTransactionCount(1);
        transaction.setTransactionsInTimeWindow(1);
        transaction.setRepeatedTransactionsToRecipient(1);

        transaction.setRiskScore(riskScore);
        transaction.setRiskLevel(
                riskScore >= 60 ? "HIGH" : "SAFE"
        );

        transaction.setFraudAction(fraudAction);

        transaction.setReasons(
                List.of("Test transaction")
        );

        transaction.setAnalyzedAt(analyzedAt);

        return transaction;
    }
}