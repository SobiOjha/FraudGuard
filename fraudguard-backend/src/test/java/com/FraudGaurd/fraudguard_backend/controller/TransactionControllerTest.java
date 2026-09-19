package com.FraudGaurd.fraudguard_backend.controller;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidFraudActionException;
import com.FraudGaurd.fraudguard_backend.ExceptionHandler.TransactionNotFoundException;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import com.FraudGaurd.fraudguard_backend.dto.DashboardStatsResponse;
import com.FraudGaurd.fraudguard_backend.model.Transaction;
import com.FraudGaurd.fraudguard_backend.service.FraudAction;
import com.FraudGaurd.fraudguard_backend.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;


    @Test
    void analyzeTransactionShouldReturnAnalysis() throws Exception {

        AnalyzeTransactionResponse response =
                new AnalyzeTransactionResponse(
                        65,
                        "HIGH",
                        List.of("New device"),
                        FraudAction.BLOCK
                );

        when(transactionService.analyzeTransaction(any()))
                .thenReturn(response);

        String requestBody = """
                {
                    "userId": "U1001",
                    "amount": 50000,
                    "currency": "INR",
                    "recipient": "RECIPIENT-01",
                    "transactionType": "TRANSFER",
                    "location": "Delhi",
                    "deviceId": "DEVICE-01",
                    "protectionMode": "PROTECTED"
                }
                """;

        mockMvc.perform(
                        post("/api/transactions/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(65))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.action").value("BLOCK"))
                .andExpect(jsonPath("$.reasons[0]").value("New device"));
    }


    @Test
    void analyzeTransactionShouldRejectMissingUserId() throws Exception {

        String requestBody = """
                {
                    "amount": 1000,
                    "currency": "INR",
                    "recipient": "RECIPIENT-01",
                    "transactionType": "TRANSFER",
                    "location": "Delhi",
                    "deviceId": "DEVICE-01",
                    "protectionMode": "PROTECTED"
                }
                """;

        mockMvc.perform(
                        post("/api/transactions/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.userId")
                                .value("User ID is required")
                );
    }


    @Test
    void analyzeTransactionShouldRejectInvalidAmount() throws Exception {

        String requestBody = """
                {
                    "userId": "U1001",
                    "amount": 0,
                    "currency": "INR",
                    "recipient": "RECIPIENT-01",
                    "transactionType": "TRANSFER",
                    "location": "Delhi",
                    "deviceId": "DEVICE-01",
                    "protectionMode": "PROTECTED"
                }
                """;

        mockMvc.perform(
                        post("/api/transactions/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.amount")
                                .value("Amount must be greater than 0")
                );
    }


    @Test
    void analyzeTransactionShouldRejectMissingRecipient() throws Exception {

        String requestBody = """
                {
                    "userId": "U1001",
                    "amount": 1000,
                    "currency": "INR",
                    "transactionType": "TRANSFER",
                    "location": "Delhi",
                    "deviceId": "DEVICE-01",
                    "protectionMode": "PROTECTED"
                }
                """;

        mockMvc.perform(
                        post("/api/transactions/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.recipient")
                                .value("Recipient is required")
                );
    }


    @Test
    void analyzeTransactionShouldRejectMissingProtectionMode()
            throws Exception {

        String requestBody = """
                {
                    "userId": "U1001",
                    "amount": 1000,
                    "currency": "INR",
                    "recipient": "RECIPIENT-01",
                    "transactionType": "TRANSFER",
                    "location": "Delhi",
                    "deviceId": "DEVICE-01"
                }
                """;

        mockMvc.perform(
                        post("/api/transactions/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.protectionMode")
                                .value("Protection mode is required")
                );
    }


    @Test
    void getAllTransactionsShouldReturnTransactions()
            throws Exception {

        Transaction transaction1 = new Transaction();
        transaction1.setUserId("U1001");

        Transaction transaction2 = new Transaction();
        transaction2.setUserId("U1002");

        when(transactionService.getAllTransactions())
                .thenReturn(List.of(transaction1, transaction2));

        mockMvc.perform(
                        get("/api/transactions")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }


    @Test
    void getDashboardStatsShouldReturnStatistics()
            throws Exception {

        DashboardStatsResponse response =
                new DashboardStatsResponse(
                        20,
                        6,
                        42.5,
                        10,
                        3
                );

        when(transactionService.getDashboardStats())
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/transactions/dashboard/stats")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalTransactions")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.highRiskTransactions")
                                .value(6)
                )
                .andExpect(
                        jsonPath("$.averageRiskScore")
                                .value(42.5)
                )
                .andExpect(
                        jsonPath("$.safeTransactions")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.blockedTransactions")
                                .value(3)
                );
    }


    @Test
    void updateFraudActionShouldReturnUpdatedTransaction()
            throws Exception {

        Transaction transaction = new Transaction();

        transaction.setUserId("U1001");
        transaction.setFraudAction("BLOCK");

        when(transactionService.updateFraudAction(
                eq(1L),
                eq("BLOCK")
        )).thenReturn(transaction);

        String requestBody = """
                {
                    "action": "BLOCK"
                }
                """;

        mockMvc.perform(
                        put("/api/transactions/1/fraud-action")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.userId")
                                .value("U1001")
                )
                .andExpect(
                        jsonPath("$.fraudAction")
                                .value("BLOCK")
                );
    }


    @Test
    void updateFraudActionShouldReturnBadRequestForInvalidAction()
            throws Exception {

        when(transactionService.updateFraudAction(
                eq(1L),
                eq("INVALID")
        )).thenThrow(
                new InvalidFraudActionException(
                        "Invalid fraud action: INVALID"
                )
        );

        String requestBody = """
                {
                    "action": "INVALID"
                }
                """;

        mockMvc.perform(
                        put("/api/transactions/1/fraud-action")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value("Invalid fraud action: INVALID")
                );
    }


    @Test
    void updateFraudActionShouldReturnNotFoundForMissingTransaction()
            throws Exception {

        when(transactionService.updateFraudAction(
                eq(999L),
                eq("BLOCK")
        )).thenThrow(
                new TransactionNotFoundException(
                        "Transaction not found with ID: 999"
                )
        );

        String requestBody = """
                {
                    "action": "BLOCK"
                }
                """;

        mockMvc.perform(
                        put("/api/transactions/999/fraud-action")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Transaction not found with ID: 999"
                                )
                );
    }
}