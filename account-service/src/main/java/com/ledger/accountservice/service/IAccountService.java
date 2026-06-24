package com.eventledger.account.service;

import com.eventledger.account.dto.request.TransactionRequest;
import com.eventledger.account.dto.response.AccountDetails;
import com.eventledger.account.dto.response.BalanceResponse;
import com.eventledger.account.dto.response.TransactionResponse;

public interface IAccountService {

    /**
     * Apply a transaction to an account (idempotent based on eventId)
     *
     * @param accountId - The account ID to apply transaction to
     * @param transactionRequest - The transaction request details
     * @param traceId - Distributed trace ID for logging
     * @return TransactionResponse with transaction details
     */
    TransactionResponse applyTransaction(String accountId, TransactionRequest transactionRequest, String traceId);

    /**
     * Get current balance for an account
     *
     * @param accountId - The account ID to fetch balance for
     * @param traceId - Distributed trace ID for logging
     * @return BalanceResponse with account balance
     */
    BalanceResponse getBalance(String accountId, String traceId);

    /**
     * Get account details with recent transactions
     *
     * @param accountId - The account ID to fetch details for
     * @param traceId - Distributed trace ID for logging
     * @return AccountDetails with full account information
     */
    AccountDetails getAccountDetails(String accountId, String traceId);
}