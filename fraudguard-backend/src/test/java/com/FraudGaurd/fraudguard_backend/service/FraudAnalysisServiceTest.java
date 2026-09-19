package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionRequest;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import com.FraudGaurd.fraudguard_backend.model.ProtectionMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FraudAnalysisServiceTest {

    private final FraudAnalysisService fraudAnalysisService =
            new FraudAnalysisService();

    @Test
    void shouldReturnSafeForNormalTransaction() {

        AnalyzeTransactionRequest request =
                createRequest(1000, "Delhi");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        1,
                        1,
                        1,
                        false,
                        false
                );

        assertEquals(0, response.getRiskScore());
        assertEquals("SAFE", response.getRiskLevel());
        assertTrue(response.getReasons().isEmpty());
        assertEquals(
                FraudAction.APPROVE,
                response.getAction()
        );
    }

    @Test
    void shouldAddRiskForHighTransactionAmount() {

        AnalyzeTransactionRequest request =
                createRequest(50000, "Delhi");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        1,
                        1,
                        1,
                        false,
                        false
                );

        assertEquals(20, response.getRiskScore());
        assertEquals("SAFE", response.getRiskLevel());

        assertTrue(
                response.getReasons().contains(
                        "Unusually high transaction amount"
                )
        );
    }

    @Test
    void shouldAddRiskForNewRecipient() {

        AnalyzeTransactionRequest request =
                createRequest(1000, "Delhi");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        1,
                        1,
                        1,
                        true,
                        false
                );

        assertEquals(15, response.getRiskScore());
        assertEquals("SAFE", response.getRiskLevel());

        assertTrue(
                response.getReasons().contains(
                        "Transaction is being made to a new recipient"
                )
        );
    }

    @Test
    void shouldAddRiskForNewDevice() {

        AnalyzeTransactionRequest request =
                createRequest(1000, "Delhi");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        1,
                        1,
                        1,
                        false,
                        true
                );

        assertEquals(20, response.getRiskScore());
        assertEquals("SAFE", response.getRiskLevel());

        assertTrue(
                response.getReasons().contains(
                        "Transaction originated from a new device"
                )
        );
    }

    @Test
    void shouldClassifyHighRiskTransaction() {

        AnalyzeTransactionRequest request =
                createRequest(50000, "Mumbai");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        10,
                        5,
                        5,
                        true,
                        true
                );

        assertEquals(100, response.getRiskScore());
        assertEquals("CRITICAL", response.getRiskLevel());

        assertFalse(response.getReasons().isEmpty());
    }

    @Test
    void shouldAddRiskForHighRecentTransactionFrequency() {

        AnalyzeTransactionRequest request =
                createRequest(1000, "Delhi");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        10,
                        1,
                        1,
                        false,
                        false
                );

        assertEquals(15, response.getRiskScore());

        assertTrue(
                response.getReasons().contains(
                        "High recent transaction frequency"
                )
        );
    }

    @Test
    void shouldAddRiskForTransactionBurst() {

        AnalyzeTransactionRequest request =
                createRequest(1000, "Delhi");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        1,
                        5,
                        1,
                        false,
                        false
                );

        assertEquals(15, response.getRiskScore());

        assertTrue(
                response.getReasons().contains(
                        "Unusually high transaction activity within a short time"
                )
        );
    }

    @Test
    void shouldAddRiskForRepeatedRecipient() {

        AnalyzeTransactionRequest request =
                createRequest(1000, "Delhi");

        AnalyzeTransactionResponse response =
                fraudAnalysisService.analyze(
                        request,
                        1,
                        1,
                        5,
                        false,
                        false
                );

        assertEquals(10, response.getRiskScore());

        assertTrue(
                response.getReasons().contains(
                        "Repeated transactions to the same recipient"
                )
        );
    }

    private AnalyzeTransactionRequest createRequest(
            double amount,
            String location) {

        AnalyzeTransactionRequest request =
                new AnalyzeTransactionRequest();

        request.setUserId("U1001");
        request.setAmount(amount);
        request.setCurrency("INR");
        request.setRecipient("RECIPIENT-01");
        request.setTransactionType("TRANSFER");
        request.setLocation(location);
        request.setDeviceId("DEVICE-01");
        request.setProtectionMode(
                ProtectionMode.PROTECTED
        );

        return request;
    }
}