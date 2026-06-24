package com.eventledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error message", example = "Validation failed")
    private String message;

    @Schema(description = "Detailed error description")
    private String detail;

    @Schema(description = "Timestamp of error", example = "2026-06-24T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Trace ID for debugging", example = "trace-12345")
    private String traceId;
}
