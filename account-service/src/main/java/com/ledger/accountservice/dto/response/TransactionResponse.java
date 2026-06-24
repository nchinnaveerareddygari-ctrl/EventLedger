package com.eventledger.account.dto.response;

import com.eventledger.account.dto.Transaction;
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
public class TransactionResponse extends Transaction {

    @JsonProperty("id")
    private String id; // Internal transaction ID

    @JsonProperty("createdAt")
    private LocalDateTime createdAt; // When recorded in system

    @JsonProperty("status")
    private String status; // APPLIED or DUPLICATE
}