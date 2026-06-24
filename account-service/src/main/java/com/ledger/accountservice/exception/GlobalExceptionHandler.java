package com.eventledger.account.exception;

import com.eventledger.account.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle custom AccountServiceException
     */
    @ExceptionHandler(AccountServiceException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceException(
            AccountServiceException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        log.error("[{}] AccountServiceException occurred: {} - {}", traceId, ex.getErrorCode(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(null)
                .build();

        HttpStatus httpStatus = HttpStatus.valueOf(ex.getHttpStatus());
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    /**
     * Handle AccountNotFoundException specifically
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(
            AccountNotFoundException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        log.warn("[{}] AccountNotFoundException: {}", traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(null)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle ValidationException specifically
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        log.warn("[{}] ValidationException: {}", traceId, ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("validationErrors", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle DuplicateEventException specifically
     */
    @ExceptionHandler(DuplicateEventException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEventException(
            DuplicateEventException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        log.warn("[{}] DuplicateEventException: {}", traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(null)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle MethodArgumentNotValidException (validation errors from @Valid annotation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[{}] Validation failed: {}", traceId, errorMessage);

        Map<String, Object> details = new HashMap<>();
        details.put("field", ex.getBindingResult().getFieldError() != null ?
                ex.getBindingResult().getFieldError().getField() : "unknown");
        details.put("rejectedValue", ex.getBindingResult().getFieldError() != null ?
                ex.getBindingResult().getFieldError().getRejectedValue() : null);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(
            NullPointerException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        log.error("[{}] NullPointerException occurred", traceId, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("NULL_POINTER_ERROR")
                .message("An unexpected error occurred while processing your request")
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(null)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        log.warn("[{}] IllegalArgumentException: {}", traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(null)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);

        log.error("[{}] Unexpected exception occurred", traceId, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please contact support with trace ID: " + traceId)
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .details(null)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Extract trace ID from request headers or generate new one
     */
    private String extractTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-ID");
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }
}