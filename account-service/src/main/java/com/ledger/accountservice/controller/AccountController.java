package com.eventledger.account.controller;

import com.eventledger.account.dto.request.TransactionRequest;
import com.eventledger.account.dto.response.*;
import com.eventledger.account.service.IAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Accounts", description = "Account operations and queries")
public class AccountController {

    @Autowired
    private IAccountService accountService;

    /**
     * Health check endpoint
     * GET /health
     */
    @GetMapping("/health")
    @Operation(
            summary = "Health check endpoint",
            description = "Returns the health status of the Account Service and its dependencies",
            operationId = "getHealth",
            tags = {"Health"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service is healthy",
                    content = @Content(schema = @Schema(implementation = HealthResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service is unhealthy",
                    content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    })
    public ResponseEntity<HealthResponse> getHealth(HttpServletRequest request) {
        String traceId = extractTraceId(request);
        log.info("[{}] Health check requested", traceId);

        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .service("account-service")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get account details including balance and recent transactions
     * GET /accounts/{accountId}
     */
    @GetMapping("/accounts/{accountId}")
    @Operation(
            summary = "Get account details",
            description = "Retrieves account information including current balance and recent transactions",
            operationId = "getAccount"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountDetails.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AccountDetails> getAccount(
            @PathVariable("accountId")
            @Parameter(description = "The unique account identifier", example = "acct-123")
            String accountId,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.info("[{}] Get account details request - accountId: {}", traceId, accountId);

        AccountDetails details = accountService.getAccountDetails(accountId, traceId);
        return ResponseEntity.ok(details);
    }

    /**
     * Get current account balance
     * GET /accounts/{accountId}/balance
     */
    @GetMapping("/accounts/{accountId}/balance")
    @Operation(
            summary = "Get current account balance",
            description = "Returns the current balance for an account (sum of credits minus debits)",
            operationId = "getAccountBalance"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BalanceResponse> getAccountBalance(
            @PathVariable("accountId")
            @Parameter(description = "The unique account identifier", example = "acct-123")
            String accountId,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.info("[{}] Get balance request - accountId: {}", traceId, accountId);

        BalanceResponse balance = accountService.getBalance(accountId, traceId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Apply a transaction to an account (idempotent based on eventId)
     * POST /accounts/{accountId}/transactions
     */
    @PostMapping("/accounts/{accountId}/transactions")
    @Operation(
            summary = "Apply a transaction to an account",
            description = "Processes a debit or credit transaction and updates the account balance. Idempotent based on eventId.",
            operationId = "applyTransaction",
            tags = {"Transactions"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction applied successfully (duplicate - idempotent)",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "201", description = "Transaction created successfully (new transaction)",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid transaction data (missing fields, invalid type, negative amount, etc.)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - EventId already exists for a different transaction",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransactionResponse> applyTransaction(
            @PathVariable("accountId")
            @Parameter(description = "The unique account identifier", example = "acct-123")
            String accountId,
            @RequestBody @Valid TransactionRequest transactionRequest,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.info("[{}] Apply transaction request - accountId: {}, eventId: {}",
                traceId, accountId, transactionRequest.getEventId());

        TransactionResponse response = accountService.applyTransaction(accountId, transactionRequest, traceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Extract trace ID from request headers or generate new one
     */
    private String extractTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-ID");
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString();
            log.debug("Generated new trace ID: {}", traceId);
        }
        return traceId;
    }
}