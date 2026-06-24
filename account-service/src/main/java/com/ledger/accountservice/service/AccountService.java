package com.eventledger.account.service.impl;

import com.eventledger.account.dto.request.TransactionRequest;
import com.eventledger.account.dto.response.AccountDetails;
import com.eventledger.account.dto.response.BalanceResponse;
import com.eventledger.account.dto.response.TransactionResponse;
import com.eventledger.account.entity.Account;
import com.eventledger.account.entity.Transaction;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import com.eventledger.account.service.IAccountService;
import com.eventledger.account.service.validation.TransactionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AccountService implements IAccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionValidator transactionValidator;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Apply a transaction to an account (idempotent based on eventId)
     */
    @Override
    public TransactionResponse applyTransaction(String accountId, TransactionRequest transactionRequest, String traceId) {
        log.info("[{}] Applying transaction - accountId: {}, eventId: {}, type: {}, amount: {}",
                traceId, accountId, transactionRequest.getEventId(), transactionRequest.getType(), transactionRequest.getAmount());

        // Step 1: Validate transaction request
        transactionValidator.validate(transactionRequest, traceId);
        log.debug("[{}] Transaction validation passed", traceId);

        // Step 2: Check for duplicate eventId (idempotency)
        Optional<Transaction> existingTransaction = transactionRepository.findByEventId(transactionRequest.getEventId());
        if (existingTransaction.isPresent()) {
            log.warn("[{}] Duplicate event detected - eventId: {}, returning existing transaction with status DUPLICATE",
                    traceId, transactionRequest.getEventId());
            Transaction existingTxn = existingTransaction.get();
            existingTxn.setStatus("DUPLICATE");
            return mapToResponse(existingTxn);
        }

        // Step 3: Get or create account
        Account account = accountRepository.findById(accountId)
                .orElseGet(() -> createNewAccount(accountId, traceId));

        if (account == null) {
            log.error("[{}] Account not found or could not be created - accountId: {}", traceId, accountId);
            throw new AccountNotFoundException(accountId);
        }

        // Step 4: Create transaction entity
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .eventId(transactionRequest.getEventId())
                .accountId(accountId)
                .type(transactionRequest.getType().toUpperCase())
                .amount(transactionRequest.getAmount())
                .currency(transactionRequest.getCurrency().toUpperCase())
                .eventTimestamp(transactionRequest.getEventTimestamp())
                .createdAt(LocalDateTime.now())
                .status("APPLIED")
                .metadata(serializeMetadata(transactionRequest.getMetadata()))
                .build();

        // Step 5: Add and save transaction
        transactionRepository.save(transaction);
        log.debug("[{}] Transaction saved to database - transactionId: {}", traceId, transaction.getId());

        // Step 6: Update account balance
        updateAccountBalance(accountId, traceId);

        log.info("[{}] Transaction successfully applied - transactionId: {}",
                traceId, transaction.getId());

        return mapToResponse(transaction);
    }

    /**
     * Get current balance for an account
     */
    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId, String traceId) {
        log.info("[{}] Fetching balance - accountId: {}", traceId, accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Double balance = calculateBalance(accountId, traceId);

        log.debug("[{}] Balance calculated - accountId: {}, balance: {}", traceId, accountId, balance);

        return BalanceResponse.builder()
                .accountId(accountId)
                .balance(balance)
                .currency(account.getCurrency())
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get account details with recent transactions
     */
    @Override
    @Transactional(readOnly = true)
    public AccountDetails getAccountDetails(String accountId, String traceId) {
        log.info("[{}] Fetching account details - accountId: {}", traceId, accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Double balance = calculateBalance(accountId, traceId);

        List<TransactionResponse> recentTransactions = transactionRepository
                .findRecentTransactionsByAccountId(accountId, 10)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        int transactionCount = (int) transactionRepository.findAllByAccountId(accountId).stream().count();

        log.debug("[{}] Account details retrieved - accountId: {}, transactionCount: {}",
                traceId, accountId, transactionCount);

        return AccountDetails.builder()
                .accountId(accountId)
                .balance(balance)
                .currency(account.getCurrency())
                .transactionCount(transactionCount)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .recentTransactions(recentTransactions)
                .build();
    }

    /**
     * Create a new account
     */
    private Account createNewAccount(String accountId, String traceId) {
        log.info("[{}] Creating new account - accountId: {}", traceId, accountId);

        Account account = Account.builder()
                .accountId(accountId)
                .balance(0.0)
                .currency("USD")
                .transactionCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

    /**
     * Calculate balance from all transactions
     */
    private Double calculateBalance(String accountId, String traceId) {
        List<Transaction> transactions = transactionRepository.findTransactionsByAccountId(accountId);

        if (transactions == null || transactions.isEmpty()) {
            log.debug("[{}] No transactions found for account {}, balance is 0", traceId, accountId);
            return 0.0;
        }

        Double balance = transactions.stream()
                .mapToDouble(txn -> {
                    if ("CREDIT".equalsIgnoreCase(txn.getType())) {
                        return txn.getAmount();
                    } else if ("DEBIT".equalsIgnoreCase(txn.getType())) {
                        return -txn.getAmount();
                    }
                    return 0.0;
                })
                .sum();

        balance = Math.round(balance * 100.0) / 100.0;

        log.debug("[{}] Balance calculated for account {} - balance: {}, transactionCount: {}",
                traceId, accountId, balance, transactions.size());

        return balance;
    }

    /**
     * Update account balance and transaction count
     */
    private void updateAccountBalance(String accountId, String traceId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Double balance = calculateBalance(accountId, traceId);
        int transactionCount = (int) transactionRepository.findAllByAccountId(accountId).stream().count();

        account.setBalance(balance);
        account.setTransactionCount(transactionCount);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.debug("[{}] Account balance updated - accountId: {}, newBalance: {}",
                traceId, accountId, balance);
    }

    /**
     * Map Transaction entity to TransactionResponse DTO
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .eventId(transaction.getEventId())
                .accountId(transaction.getAccountId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .eventTimestamp(transaction.getEventTimestamp())
                .createdAt(transaction.getCreatedAt())
                .status(transaction.getStatus())
                .metadata(deserializeMetadata(transaction.getMetadata()))
                .build();
    }

    /**
     * Serialize metadata to JSON string
     */
    private String serializeMetadata(Object metadata) {
        try {
            if (metadata == null) {
                return null;
            }
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata", e);
            return null;
        }
    }

    /**
     * Deserialize metadata from JSON string
     */
    private java.util.Map<String, Object> deserializeMetadata(String metadata) {
        try {
            if (metadata == null) {
                return null;
            }
            return objectMapper.readValue(metadata, java.util.Map.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize metadata", e);
            return null;
        }
    }
}