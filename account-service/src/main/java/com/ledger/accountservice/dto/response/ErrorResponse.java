package com.eventledger.account.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    @JsonProperty("error")
    private String error; // Error code or type

    @JsonProperty("message")
    private String message; // Human-readable message

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("traceId")
    private String traceId; // Distributed trace ID

    @JsonProperty("details")
    private Map<String, Object> details; // Additional error details
}