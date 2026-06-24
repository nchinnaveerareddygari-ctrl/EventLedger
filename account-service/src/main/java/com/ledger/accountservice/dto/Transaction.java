package com.eventledger.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @NotBlank(message = "Event ID is required")
    @JsonProperty("eventId")
    private String eventId;

    @NotBlank(message = "Account ID is required")
    @JsonProperty("accountId")
    private String accountId;

    @NotNull(message = "Transaction type is required")
    @JsonProperty("type")
    private String type; // CREDIT or DEBIT

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @JsonProperty("amount")
    private Double amount;

    @NotBlank(message = "Currency is required")
    @JsonProperty("currency")
    private String currency; // ISO 4217 code

    @NotNull(message = "Event timestamp is required")
    @JsonProperty("eventTimestamp")
    private LocalDateTime eventTimestamp;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}