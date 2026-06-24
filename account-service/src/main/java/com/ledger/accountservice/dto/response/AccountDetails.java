package com.eventledger.account.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDetails {

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("balance")
    private Double balance; // Current net balance

    @JsonProperty("currency")
    private String currency; // ISO 4217 code, defaults to USD

    @JsonProperty("transactionCount")
    private Integer transactionCount; // Total number of transactions

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonProperty("recentTransactions")
    private List<TransactionResponse> recentTransactions; // Up to 10 most recent transactions
}