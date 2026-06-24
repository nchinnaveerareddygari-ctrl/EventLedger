package com.eventledger.controller;

import com.eventledger.dto.ErrorResponse;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Financial transaction event endpoints")
public class EventController {

    @Autowired
    private EventService eventService;

    /**
     * Submit a transaction event
     */
    @PostMapping
    @Operation(summary = "Submit a transaction event", description = "Creates a new transaction event. Enforces idempotency.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate event (idempotent)",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> submitEvent(@Valid @RequestBody EventRequest request) {
        try {
            EventResponse response = eventService.processEvent(request);
            log.info("Event submitted successfully: {}", request.getEventId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null));
        } catch (RuntimeException e) {
            log.error("Service error: {}", e.getMessage(), e);
            if (e.getMessage().contains("unavailable")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                                "Account Service is temporarily unavailable", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal server error", e.getMessage()));
        }
    }

    /**
     * Retrieve a single event by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieves a single event from the gateway")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> getEvent(
            @Parameter(description = "Event ID")
            @PathVariable Long id) {
        Optional<EventResponse> event = eventService.getEventById(id);
        if (event.isPresent()) {
            log.info("Event retrieved: {}", id);
            return ResponseEntity.ok(event.get());
        }
        log.warn("Event not found: {}", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(HttpStatus.NOT_FOUND, "Event not found", null));
    }

    /**
     * Retrieve all events for an account
     * Ordered by event timestamp (chronological order)
     */
    @GetMapping
    @Operation(summary = "List events for an account",
            description = "Retrieves all events for a specific account, ordered by event timestamp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing accountId parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> listEventsByAccount(
            @Parameter(description = "Account ID", required = true)
            @RequestParam String account) {
        if (account == null || account.isBlank()) {
            log.warn("Missing accountId parameter");
            return ResponseEntity.badRequest()
                    .body(buildErrorResponse(HttpStatus.BAD_REQUEST,
                            "account parameter is required", null));
        }

        List<EventResponse> events = eventService.getEventsByAccount(account);
        log.info("Retrieved {} events for account: {}", events.size(), account);
        return ResponseEntity.ok(events);
    }

    /**
     * Error handler for validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors));
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, String detail) {
        return ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .detail(detail)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
