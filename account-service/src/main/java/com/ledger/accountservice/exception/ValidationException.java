package com.eventledger.account.exception;

public class ValidationException extends AccountServiceException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message, 400);
    }
}