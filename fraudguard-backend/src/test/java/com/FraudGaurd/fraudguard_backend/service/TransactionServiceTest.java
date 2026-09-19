package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidFraudActionException;
import com.FraudGaurd.fraudguard_backend.ExceptionHandler.TransactionNotFoundException;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionRequest;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import com.FraudGaurd.fraudguard_backend.dto.DashboardStatsResponse;
import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;
import com.FraudGaurd.fraudguard_backend.model.Transaction;
import com.FraudGaurd.fraudguard_backend.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudAnalysisService fraudAnalysisService;

    @Mock
    private FraudProtectionService fraudProtectionService;

    @InjectMocks
    private TransactionService transactionService;


    @Test
    void analyzeTransactionShouldSaveTransaction() {

        AnalyzeTransactionRequest request =
                createRequest();

        AnalyzeTransactionResponse analysis =
                new AnalyzeTransactionResponse(
                        65,
                        "HIGH",
                        List.of("New device"),
                        FraudAction.APPROVE
                );

        when(transactionRepository.countRecentTransactions(
                eq("U1001"),
                any()
        )).thenReturn(5L);

        when(transactionRepository.countTransactionsToRecipient(
                eq("U1001"),
                eq("RECIPIENT-01"),
                any()
        )).thenReturn(2L);

        when(transactionRepository.countPreviousTransactionsToRecipient(
                "U1001",
                "RECIPIENT-01"
        )).thenReturn(0L);

        when(transactionRepository.countPreviousTransactionsFromDevice(
                "U1001",
                "DEVICE-01"
        )).thenReturn(0L);

        when(fraudAnalysisService.analyze(
                eq(request),
                eq(5),
                eq(5),
                eq(2),
                eq(true),
                eq(true)
        )).thenReturn(analysis);

        when(fraudProtectionService.decideAction(
                65,
                ProtectionMode.PROTECTED
        )).thenReturn(FraudAction.BLOCK);

        Transaction savedTransaction = new Transaction();

        savedTransaction.setRiskScore(65);
        savedTransaction.setRiskLevel("HIGH");
        savedTransaction.setFraudAction("BLOCK");

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        AnalyzeTransactionResponse response =
                transactionService.analyzeTransaction(request);

        assertEquals(65, response.getRiskScore());
        assertEquals("HIGH", response.getRiskLevel());
        assertEquals(FraudAction.BLOCK, response.getAction());

        verify(transactionRepository).save(any(Transaction.class));
    }


    @Test
    void analyzeTransactionShouldStoreCalculatedBehavioralData() {

        AnalyzeTransactionRequest request =
                createRequest();

        AnalyzeTransactionResponse analysis =
                new AnalyzeTransactionResponse(
                        40,
                        "SUSPICIOUS",
                        List.of("New recipient"),
                        FraudAction.APPROVE
                );

        when(transactionRepository.countRecentTransactions(
                eq("U1001"),
                any()
        )).thenReturn(7L);

        when(transactionRepository.countTransactionsToRecipient(
                eq("U1001"),
                eq("RECIPIENT-01"),
                any()
        )).thenReturn(3L);

        when(transactionRepository.countPreviousTransactionsToRecipient(
                "U1001",
                "RECIPIENT-01"
        )).thenReturn(0L);

        when(transactionRepository.countPreviousTransactionsFromDevice(
                "U1001",
                "DEVICE-01"
        )).thenReturn(1L);

        when(fraudAnalysisService.analyze(
                eq(request),
                eq(7),
                eq(7),
                eq(3),
                eq(true),
                eq(false)
        )).thenReturn(analysis);

        when(fraudProtectionService.decideAction(
                40,
                ProtectionMode.PROTECTED
        )).thenReturn(FraudAction.FLAG);

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        Transaction savedTransaction = new Transaction();

        when(transactionRepository.save(transactionCaptor.capture()))
                .thenReturn(savedTransaction);

        transactionService.analyzeTransaction(request);

        Transaction transaction =
                transactionCaptor.getValue();

        assertEquals("U1001", transaction.getUserId());
        assertEquals(7, transaction.getRecentTransactionCount());
        assertEquals(7, transaction.getTransactionsInTimeWindow());
        assertEquals(3, transaction.getRepeatedTransactionsToRecipient());

        assertTrue(transaction.isNewRecipient());
        assertFalse(transaction.isNewDevice());

        assertEquals(40, transaction.getRiskScore());
        assertEquals("SUSPICIOUS", transaction.getRiskLevel());
        assertEquals("FLAG", transaction.getFraudAction());
    }


    @Test
    void getDashboardStatsShouldReturnCorrectStatistics() {

        when(transactionRepository.count())
                .thenReturn(20L);

        when(transactionRepository.countByRiskScoreGreaterThanEqual(60))
                .thenReturn(6L);

        when(transactionRepository.countByRiskScoreLessThan(30))
                .thenReturn(10L);

        when(transactionRepository.countByFraudAction("BLOCK"))
                .thenReturn(3L);

        when(transactionRepository.findAverageRiskScore())
                .thenReturn(42.5);

        DashboardStatsResponse response =
                transactionService.getDashboardStats();

        assertEquals(20, response.getTotalTransactions());
        assertEquals(6, response.getHighRiskTransactions());
        assertEquals(42.5, response.getAverageRiskScore());
        assertEquals(10, response.getSafeTransactions());
        assertEquals(3, response.getBlockedTransactions());
    }


    @Test
    void getDashboardStatsShouldReturnZeroAverageWhenNoTransactions() {

        when(transactionRepository.count())
                .thenReturn(0L);

        when(transactionRepository.countByRiskScoreGreaterThanEqual(60))
                .thenReturn(0L);

        when(transactionRepository.countByRiskScoreLessThan(30))
                .thenReturn(0L);

        when(transactionRepository.countByFraudAction("BLOCK"))
                .thenReturn(0L);

        when(transactionRepository.findAverageRiskScore())
                .thenReturn(null);

        DashboardStatsResponse response =
                transactionService.getDashboardStats();

        assertEquals(0, response.getTotalTransactions());
        assertEquals(0, response.getHighRiskTransactions());
        assertEquals(0.0, response.getAverageRiskScore());
        assertEquals(0, response.getSafeTransactions());
        assertEquals(0, response.getBlockedTransactions());
    }


    @Test
    void getAllTransactionsShouldReturnTransactions() {

        Transaction transaction1 = new Transaction();
        Transaction transaction2 = new Transaction();

        when(transactionRepository.findAll())
                .thenReturn(List.of(transaction1, transaction2));

        List<Transaction> transactions =
                transactionService.getAllTransactions();

        assertEquals(2, transactions.size());

        verify(transactionRepository).findAll();
    }


    @Test
    void updateFraudActionShouldUpdateValidAction() {

        Transaction transaction = new Transaction();

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        Transaction result =
                transactionService.updateFraudAction(
                        1L,
                        "BLOCK"
                );

        assertEquals("BLOCK", result.getFraudAction());

        verify(transactionRepository).save(transaction);
    }


    @Test
    void updateFraudActionShouldThrowWhenTransactionDoesNotExist() {

        when(transactionRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.updateFraudAction(
                        999L,
                        "BLOCK"
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void updateFraudActionShouldThrowForInvalidAction() {

        Transaction transaction = new Transaction();

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        assertThrows(
                InvalidFraudActionException.class,
                () -> transactionService.updateFraudAction(
                        1L,
                        "INVALID"
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    private AnalyzeTransactionRequest createRequest() {

        AnalyzeTransactionRequest request =
                new AnalyzeTransactionRequest();

        request.setUserId("U1001");
        request.setAmount(1000);
        request.setCurrency("INR");
        request.setRecipient("RECIPIENT-01");
        request.setTransactionType("TRANSFER");
        request.setLocation("Delhi");
        request.setDeviceId("DEVICE-01");
        request.setProtectionMode(
                ProtectionMode.PROTECTED
        );

        return request;
    }
}