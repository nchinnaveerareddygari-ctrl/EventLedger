package com.ledger.accountservice.exception;

import com.eventledger.account.dto.response.ErrorResponse;
import com.eventledger.account.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("X-Trace-ID")).thenReturn("test-trace-id");
    }

    @Test
    @DisplayName("should handle AccountServiceException")
    void shouldHandleAccountServiceException() {
        AccountServiceException ex = new AccountServiceException("CUSTOM_ERROR", "Custom error occurred", 500);

        ResponseEntity<ErrorResponse> response = handler.handleAccountServiceException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("CUSTOM_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Custom error occurred");
        assertThat(response.getBody().getTraceId()).isEqualTo("test-trace-id");
    }

    @Test
    @DisplayName("should handle AccountNotFoundException")
    void shouldHandleAccountNotFoundException() {
        AccountNotFoundException ex = new AccountNotFoundException("acct-999");

        ResponseEntity<ErrorResponse> response = handler.handleAccountNotFoundException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("acct-999");
    }

    @Test
    @DisplayName("should handle ValidationException")
    void shouldHandleValidationException() {
        ValidationException ex = new ValidationException("Invalid amount");

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getDetails()).containsKey("validationErrors");
    }

    @Test
    @DisplayName("should handle DuplicateEventException")
    void shouldHandleDuplicateEventException() {
        DuplicateEventException ex = new DuplicateEventException("evt-123", "Event already processed");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateEventException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("DUPLICATE_EVENT");
    }

    @Test
    @DisplayName("should handle NullPointerException")
    void shouldHandleNullPointerException() {
        NullPointerException ex = new NullPointerException("null ref");

        ResponseEntity<ErrorResponse> response = handler.handleNullPointerException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("NULL_POINTER_ERROR");
    }

    @Test
    @DisplayName("should handle IllegalArgumentException")
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad argument");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().getMessage()).isEqualTo("Bad argument");
    }

    @Test
    @DisplayName("should handle generic Exception")
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getTraceId()).isEqualTo("test-trace-id");
    }

    @Test
    @DisplayName("should generate trace ID when header is missing")
    void shouldGenerateTraceIdWhenHeaderMissing() {
        when(mockRequest.getHeader("X-Trace-ID")).thenReturn(null);

        AccountNotFoundException ex = new AccountNotFoundException("acct-1");

        ResponseEntity<ErrorResponse> response = handler.handleAccountNotFoundException(ex, mockRequest);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTraceId()).isNotNull();
        assertThat(response.getBody().getTraceId()).isNotEmpty();
    }

    @Test
    @DisplayName("should generate trace ID when header is blank")
    void shouldGenerateTraceIdWhenHeaderBlank() {
        when(mockRequest.getHeader("X-Trace-ID")).thenReturn("   ");

        AccountNotFoundException ex = new AccountNotFoundException("acct-1");

        ResponseEntity<ErrorResponse> response = handler.handleAccountNotFoundException(ex, mockRequest);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTraceId()).isNotNull();
        assertThat(response.getBody().getTraceId()).isNotBlank();
    }

    @Test
    @DisplayName("should handle MethodArgumentNotValidException")
    void shouldHandleMethodArgumentNotValidException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "transactionRequest");
        bindingResult.addError(new FieldError("transactionRequest", "amount", null, false,
                null, null, "Amount must be greater than 0"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(ex, mockRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).contains("Amount must be greater than 0");
        assertThat(response.getBody().getDetails()).containsKey("field");
    }
}
