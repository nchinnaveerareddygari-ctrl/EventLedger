package com.eventledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Event response details")
public class EventResponse {

    @Schema(description = "Database ID", example = "1")
    private Long id;

    @Schema(description = "Unique event identifier", example = "evt-001")
    private String eventId;

    @Schema(description = "Account ID", example = "acct-123")
    private String accountId;

    @Schema(description = "Transaction type", example = "CREDIT")
    private String type;

    @Schema(description = "Transaction amount", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Event timestamp", example = "2026-05-15T14:02:11Z")
    private ZonedDateTime eventTimestamp;

    @Schema(description = "Optional metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Event status", example = "PROCESSED")
    private String status;

    @Schema(description = "When the event was created in the gateway", example = "2026-06-24T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Trace ID for distributed tracing", example = "trace-12345")
    private String traceId;
}
