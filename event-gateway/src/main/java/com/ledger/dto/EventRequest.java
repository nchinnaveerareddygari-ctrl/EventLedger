package com.eventledger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Financial transaction event payload")
public class EventRequest {

    @NotBlank(message = "eventId is required")
    @Schema(description = "Unique identifier for the event", example = "evt-001")
    private String eventId;

    @NotBlank(message = "accountId is required")
    @Schema(description = "The account this event belongs to", example = "acct-123")
    private String accountId;

    @NotNull(message = "type is required")
    @Schema(description = "Transaction type", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
    private String type;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Schema(description = "Transaction amount", example = "150.00")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be 3 characters")
    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @NotNull(message = "eventTimestamp is required")
    @Schema(description = "When the event originally occurred (ISO 8601)", example = "2026-05-15T14:02:11Z")
    private ZonedDateTime eventTimestamp;

    @Schema(description = "Optional metadata")
    private Map<String, Object> metadata;
}
