package com.ledger.accountservice.exception;

import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.exception.AccountServiceException;
import com.eventledger.account.exception.DuplicateEventException;
import com.eventledger.account.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionTest {

    @Nested
    @DisplayName("AccountServiceException")
    class AccountServiceExceptionTest {

        @Test
        @DisplayName("should create exception with errorCode, message, and httpStatus")
        void shouldCreateWithAllFields() {
            AccountServiceException ex = new AccountServiceException("TEST_ERROR", "Test message", 500);

            assertThat(ex.getErrorCode()).isEqualTo("TEST_ERROR");
            assertThat(ex.getMessage()).isEqualTo("Test message");
            assertThat(ex.getHttpStatus()).isEqualTo(500);
        }

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            AccountServiceException ex = new AccountServiceException("ERR", "msg", 400);

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should be throwable")
        void shouldBeThrowable() {
            assertThatThrownBy(() -> {
                throw new AccountServiceException("ERR", "test", 500);
            }).isInstanceOf(AccountServiceException.class)
                    .hasMessage("test");
        }
    }

    @Nested
    @DisplayName("AccountNotFoundException")
    class AccountNotFoundExceptionTest {

        @Test
        @DisplayName("should set correct error code and status")
        void shouldSetCorrectErrorCodeAndStatus() {
            AccountNotFoundException ex = new AccountNotFoundException("acct-999");

            assertThat(ex.getErrorCode()).isEqualTo("ACCOUNT_NOT_FOUND");
            assertThat(ex.getHttpStatus()).isEqualTo(404);
            assertThat(ex.getMessage()).isEqualTo("Account not found: acct-999");
        }

        @Test
        @DisplayName("should include accountId in message")
        void shouldIncludeAccountIdInMessage() {
            AccountNotFoundException ex = new AccountNotFoundException("my-account");

            assertThat(ex.getMessage()).contains("my-account");
        }

        @Test
        @DisplayName("should extend AccountServiceException")
        void shouldExtendAccountServiceException() {
            AccountNotFoundException ex = new AccountNotFoundException("acct-1");

            assertThat(ex).isInstanceOf(AccountServiceException.class);
        }
    }

    @Nested
    @DisplayName("DuplicateEventException")
    class DuplicateEventExceptionTest {

        @Test
        @DisplayName("should set correct error code and status")
        void shouldSetCorrectErrorCodeAndStatus() {
            DuplicateEventException ex = new DuplicateEventException("evt-123", "Duplicate event detected");

            assertThat(ex.getErrorCode()).isEqualTo("DUPLICATE_EVENT");
            assertThat(ex.getHttpStatus()).isEqualTo(409);
            assertThat(ex.getMessage()).isEqualTo("Duplicate event detected");
        }

        @Test
        @DisplayName("should extend AccountServiceException")
        void shouldExtendAccountServiceException() {
            DuplicateEventException ex = new DuplicateEventException("evt-1", "dup");

            assertThat(ex).isInstanceOf(AccountServiceException.class);
        }
    }

    @Nested
    @DisplayName("ValidationException")
    class ValidationExceptionTest {

        @Test
        @DisplayName("should set correct error code and status")
        void shouldSetCorrectErrorCodeAndStatus() {
            ValidationException ex = new ValidationException("Amount must be positive");

            assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(ex.getHttpStatus()).isEqualTo(400);
            assertThat(ex.getMessage()).isEqualTo("Amount must be positive");
        }

        @Test
        @DisplayName("should extend AccountServiceException")
        void shouldExtendAccountServiceException() {
            ValidationException ex = new ValidationException("invalid");

            assertThat(ex).isInstanceOf(AccountServiceException.class);
        }
    }
}
