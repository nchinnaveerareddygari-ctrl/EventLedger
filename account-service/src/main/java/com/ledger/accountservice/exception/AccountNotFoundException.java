package com.eventledger.account.exception;

public class AccountNotFoundException extends AccountServiceException {
    public AccountNotFoundException(String accountId) {
        super("ACCOUNT_NOT_FOUND", "Account not found: " + accountId, 404);
    }
}