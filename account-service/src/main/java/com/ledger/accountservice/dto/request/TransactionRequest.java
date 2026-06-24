package com.eventledger.account.dto.request;

import com.eventledger.account.dto.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest extends Transaction {
    // Inherits all fields from Transaction
    // This class can be extended with additional request-specific fields if needed
}