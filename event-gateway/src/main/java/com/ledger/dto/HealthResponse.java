package com.eventledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Health check response")
public class HealthResponse {

    @Schema(description = "Service status", example = "UP")
    private String status;

    @Schema(description = "Service name", example = "event-gateway")
    private String service;

    @Schema(description = "Database connection status", example = "CONNECTED")
    private String database;

    @Schema(description = "Account Service status", example = "UP")
    private String accountService;

    @Schema(description = "Timestamp", example = "2026-06-24T10:30:00Z")
    private String timestamp;
}
