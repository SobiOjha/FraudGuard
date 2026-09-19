package com.FraudGaurd.fraudguard_backend.controller;

import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionRequest;
import com.FraudGaurd.fraudguard_backend.dto.AnalyzeTransactionResponse;
import com.FraudGaurd.fraudguard_backend.dto.DashboardStatsResponse;
import com.FraudGaurd.fraudguard_backend.dto.UpdateFraudActionRequest;
import com.FraudGaurd.fraudguard_backend.model.Transaction;
import com.FraudGaurd.fraudguard_backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://sentinel-frontend-5l8x.onrender.com"
})
@Tag(
        name = "Transactions",
        description = "Fraud transaction analysis and management APIs"
)
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @PostMapping("/analyze")
    @Operation(
            summary = "Analyze a transaction",
            description = "Analyzes a transaction using FraudGuard's rule-based behavioral fraud detection engine."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction analyzed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid transaction data"
            )
    })
    public AnalyzeTransactionResponse analyzeTransaction(
            @Valid @RequestBody AnalyzeTransactionRequest request) {

        return transactionService.analyzeTransaction(request);
    }

    @GetMapping
    @Operation(
            summary = "Get all transactions",
            description = "Retrieves all previously analyzed transactions."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully"
    )
    public List<Transaction> getAllTransactions() {

        return transactionService.getAllTransactions();
    }

    @GetMapping("/dashboard/stats")
    @Operation(
            summary = "Get dashboard statistics",
            description = "Retrieves aggregated fraud analysis statistics for the dashboard."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Dashboard statistics retrieved successfully"
    )
    public DashboardStatsResponse getDashboardStats() {

        return transactionService.getDashboardStats();
    }

    @PutMapping("/{id}/fraud-action")
    @Operation(
            summary = "Update fraud action",
            description = "Updates the fraud action of a previously analyzed transaction."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Fraud action updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid fraud action"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found"
            )
    })
    public Transaction updateFraudAction(
            @PathVariable Long id,
            @RequestBody UpdateFraudActionRequest request) {

        return transactionService.updateFraudAction(
                id,
                request.getAction()
        );
    }
}