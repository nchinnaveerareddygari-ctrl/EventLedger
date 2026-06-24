package com.eventledger.account.exception;

public class DuplicateEventException extends AccountServiceException {
    public DuplicateEventException(String eventId, String message) {
        super("DUPLICATE_EVENT", message, 409);
    }
}