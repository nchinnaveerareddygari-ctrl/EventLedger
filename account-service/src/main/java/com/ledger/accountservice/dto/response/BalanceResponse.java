package com.eventledger.account.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("balance")
    private Double balance; // Net balance (credits - debits)

    @JsonProperty("currency")
    private String currency; // ISO 4217 code

    @JsonProperty("calculatedAt")
    private LocalDateTime calculatedAt;
}