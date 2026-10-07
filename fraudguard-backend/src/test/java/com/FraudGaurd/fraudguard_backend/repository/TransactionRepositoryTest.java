package com.FraudGaurd.fraudguard_backend.repository;

import com.FraudGaurd.fraudguard_backend.model.Integration;
import com.FraudGaurd.fraudguard_backend.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("dev")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private IntegrationRepository integrationRepository;

    private Integration integration;

    @BeforeEach
    void setUpIntegration() {
        integration = integrationRepository.save(
                new Integration("Test Integration", "test-hash"));
    }


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
                        integration,
                        "U1001",
                        now.minusHours(24)
                );

        assertEquals(1, count);
    }

    @Test
    void behavioralQueriesDoNotCrossIntegrationBoundaries() {
        Integration otherIntegration = integrationRepository.save(
                new Integration("Other Integration", "other-test-hash"));
        LocalDateTime now = LocalDateTime.now();

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        now.minusHours(1),
                        40,
                        "FLAG"
                )
        );

        Transaction otherTransaction = createTransaction(
                "U1001",
                "RECIPIENT-01",
                "DEVICE-01",
                now.minusHours(1),
                40,
                "FLAG"
        );
        otherTransaction.setIntegration(otherIntegration);
        transactionRepository.save(otherTransaction);

        assertEquals(
                1,
                transactionRepository.countRecentTransactions(
                        integration,
                        "U1001",
                        now.minusHours(24)
                )
        );
        assertEquals(
                1,
                transactionRepository.countRecentTransactions(
                        otherIntegration,
                        "U1001",
                        now.minusHours(24)
                )
        );
        assertEquals(
                1,
                transactionRepository.countTransactionsToRecipient(
                        integration,
                        "U1001",
                        "RECIPIENT-01",
                        now.minusHours(24)
                )
        );
        assertEquals(
                1,
                transactionRepository.countTransactionsToRecipient(
                        otherIntegration,
                        "U1001",
                        "RECIPIENT-01",
                        now.minusHours(24)
                )
        );
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
                        integration,
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
                        integration,
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
                                integration,
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
                                integration,
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
                        .countByIntegrationAndRiskScoreGreaterThanEqual(
                                integration,
                                60
                        )
        );

        assertEquals(
                1,
                transactionRepository
                        .countByIntegrationAndRiskScoreLessThan(
                                integration,
                                30
                        )
        );

        assertEquals(
                2,
                transactionRepository
                        .countByIntegrationAndFraudAction(
                                integration,
                                "BLOCK"
                        )
        );
    }


    @Test
    void shouldReturnMultipleTransactionsForIntegrationHistory() {

        transactionRepository.save(
                createTransaction(
                        "U1001",
                        "RECIPIENT-01",
                        "DEVICE-01",
                        LocalDateTime.now().minusMinutes(2),
                        20,
                        "APPROVE"
                )
        );

        transactionRepository.save(
                createTransaction(
                        "U1002",
                        "RECIPIENT-02",
                        "DEVICE-02",
                        LocalDateTime.now().minusMinutes(1),
                        75,
                        "FLAG"
                )
        );

        List<Transaction> history =
                transactionRepository.findAllByIntegration(integration);

        assertEquals(2, history.size());
    }


    @Test
    void nullOwnedTransactionsAreExcludedFromIntegrationScopedQueries() {

        LocalDateTime now = LocalDateTime.now();
        Transaction legacyTransaction = createTransaction(
                "U1001",
                "RECIPIENT-LEGACY",
                "DEVICE-LEGACY",
                now.minusMinutes(1),
                90,
                "BLOCK"
        );
        legacyTransaction.setIntegration(null);

        Transaction savedLegacyTransaction =
                transactionRepository.save(legacyTransaction);
        transactionRepository.flush();

        assertEquals(
                0,
                transactionRepository.findAllByIntegration(integration).size()
        );
        assertEquals(0, transactionRepository.countByIntegration(integration));
        assertEquals(
                0,
                transactionRepository
                        .countByIntegrationAndRiskScoreGreaterThanEqual(
                                integration,
                                60
                        )
        );
        assertEquals(
                0,
                transactionRepository.countByIntegrationAndRiskScoreLessThan(
                        integration,
                        30
                )
        );
        assertEquals(
                0,
                transactionRepository.countByIntegrationAndFraudAction(
                        integration,
                        "BLOCK"
                )
        );
        assertEquals(
                null,
                transactionRepository.findAverageRiskScore(integration)
        );
        assertEquals(
                0,
                transactionRepository.countRecentTransactions(
                        integration,
                        "U1001",
                        now.minusHours(24)
                )
        );
        assertEquals(
                0,
                transactionRepository.countTransactionsToRecipient(
                        integration,
                        "U1001",
                        "RECIPIENT-LEGACY",
                        now.minusHours(24)
                )
        );
        assertEquals(
                0,
                transactionRepository.countPreviousTransactionsToRecipient(
                        integration,
                        "U1001",
                        "RECIPIENT-LEGACY"
                )
        );
        assertEquals(
                0,
                transactionRepository.countPreviousTransactionsFromDevice(
                        integration,
                        "U1001",
                        "DEVICE-LEGACY"
                )
        );
        assertEquals(
                Optional.empty(),
                transactionRepository.findByIdAndIntegration(
                        savedLegacyTransaction.getId(),
                        integration
                )
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
                transactionRepository.findAverageRiskScore(integration);

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

        transaction.setIntegration(integration);
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
