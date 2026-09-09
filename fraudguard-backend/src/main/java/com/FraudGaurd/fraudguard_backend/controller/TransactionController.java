package com.FraudGaurd.fraudguard_backend.controller;

import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionRequest;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import com.FraudGaurd.fraudguard_backend.dto.DashboardStatsResponse;
import com.FraudGaurd.fraudguard_backend.dto.UpdateFraudActionRequest;
import com.FraudGaurd.fraudguard_backend.model.Transaction;
import com.FraudGaurd.fraudguard_backend.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @PostMapping("/analyze")
    public AnalyzeTransactionResponse analyzeTransaction(
            @RequestBody AnalyzeTransactionRequest request) {

        return transactionService.analyzeTransaction(request);
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {

        return transactionService.getAllTransactions();
    }

    @GetMapping("/dashboard/stats")
    public DashboardStatsResponse getDashboardStats() {

        return transactionService.getDashboardStats();
    }

    @PutMapping("/{id}/fraud-action")
    public Transaction updateFraudAction(
            @PathVariable Long id,
            @RequestBody UpdateFraudActionRequest request) {

        return transactionService.updateFraudAction(
                id,
                request.getAction()
        );
    }
}