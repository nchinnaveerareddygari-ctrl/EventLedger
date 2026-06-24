package com.ledger.accountservice.dto;

import com.eventledger.account.dto.Transaction;
import com.eventledger.account.dto.request.TransactionRequest;
import com.eventledger.account.dto.response.AccountDetails;
import com.eventledger.account.dto.response.BalanceResponse;
import com.eventledger.account.dto.response.ErrorResponse;
import com.eventledger.account.dto.response.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionDtoTest {

    @Nested
    @DisplayName("Transaction DTO")
    class TransactionDtoTests {

        @Test
        @DisplayName("should create Transaction DTO using builder")
        void shouldCreateUsingBuilder() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> metadata = Map.of("source", "api");

            Transaction dto = Transaction.builder()
                    .eventId("evt-001")
                    .accountId("acct-001")
                    .type("CREDIT")
                    .amount(100.0)
                    .currency("USD")
                    .eventTimestamp(now)
                    .metadata(metadata)
                    .build();

            assertThat(dto.getEventId()).isEqualTo("evt-001");
            assertThat(dto.getAccountId()).isEqualTo("acct-001");
            assertThat(dto.getType()).isEqualTo("CREDIT");
            assertThat(dto.getAmount()).isEqualTo(100.0);
            assertThat(dto.getCurrency()).isEqualTo("USD");
            assertThat(dto.getEventTimestamp()).isEqualTo(now);
            assertThat(dto.getMetadata()).containsEntry("source", "api");
        }

        @Test
        @DisplayName("should create Transaction DTO using no-args and setters")
        void shouldCreateUsingSetters() {
            Transaction dto = new Transaction();
            dto.setEventId("evt-002");
            dto.setType("DEBIT");
            dto.setAmount(50.0);

            assertThat(dto.getEventId()).isEqualTo("evt-002");
            assertThat(dto.getType()).isEqualTo("DEBIT");
            assertThat(dto.getAmount()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("should support null metadata")
        void shouldSupportNullMetadata() {
            Transaction dto = Transaction.builder()
                    .eventId("evt-003")
                    .metadata(null)
                    .build();

            assertThat(dto.getMetadata()).isNull();
        }
    }

    @Nested
    @DisplayName("TransactionRequest DTO")
    class TransactionRequestTests {

        @Test
        @DisplayName("should extend Transaction DTO")
        void shouldExtendTransactionDto() {
            TransactionRequest request = new TransactionRequest();
            request.setEventId("evt-001");
            request.setAccountId("acct-001");
            request.setType("CREDIT");
            request.setAmount(75.0);
            request.setCurrency("USD");
            request.setEventTimestamp(LocalDateTime.now());

            assertThat(request).isInstanceOf(Transaction.class);
            assertThat(request.getEventId()).isEqualTo("evt-001");
            assertThat(request.getAmount()).isEqualTo(75.0);
        }
    }

    @Nested
    @DisplayName("TransactionResponse DTO")
    class TransactionResponseTests {

        @Test
        @DisplayName("should create TransactionResponse with builder")
        void shouldCreateWithBuilder() {
            LocalDateTime now = LocalDateTime.now();

            TransactionResponse response = TransactionResponse.builder()
                    .id("txn-001")
                    .createdAt(now)
                    .status("APPLIED")
                    .build();

            assertThat(response.getId()).isEqualTo("txn-001");
            assertThat(response.getCreatedAt()).isEqualTo(now);
            assertThat(response.getStatus()).isEqualTo("APPLIED");
        }

        @Test
        @DisplayName("should support DUPLICATE status")
        void shouldSupportDuplicateStatus() {
            TransactionResponse response = TransactionResponse.builder()
                    .id("txn-002")
                    .status("DUPLICATE")
                    .build();

            assertThat(response.getStatus()).isEqualTo("DUPLICATE");
        }
    }

    @Nested
    @DisplayName("BalanceResponse DTO")
    class BalanceResponseTests {

        @Test
        @DisplayName("should create BalanceResponse with builder")
        void shouldCreateWithBuilder() {
            LocalDateTime now = LocalDateTime.now();

            BalanceResponse response = BalanceResponse.builder()
                    .accountId("acct-001")
                    .balance(500.50)
                    .currency("USD")
                    .calculatedAt(now)
                    .build();

            assertThat(response.getAccountId()).isEqualTo("acct-001");
            assertThat(response.getBalance()).isEqualTo(500.50);
            assertThat(response.getCurrency()).isEqualTo("USD");
            assertThat(response.getCalculatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should support zero balance")
        void shouldSupportZeroBalance() {
            BalanceResponse response = BalanceResponse.builder()
                    .accountId("acct-new")
                    .balance(0.0)
                    .currency("USD")
                    .build();

            assertThat(response.getBalance()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should support negative balance")
        void shouldSupportNegativeBalance() {
            BalanceResponse response = BalanceResponse.builder()
                    .accountId("acct-overdrawn")
                    .balance(-50.0)
                    .currency("USD")
                    .build();

            assertThat(response.getBalance()).isEqualTo(-50.0);
        }
    }

    @Nested
    @DisplayName("AccountDetails DTO")
    class AccountDetailsTests {

        @Test
        @DisplayName("should create AccountDetails with builder")
        void shouldCreateWithBuilder() {
            LocalDateTime now = LocalDateTime.now();
            List<TransactionResponse> transactions = List.of(
                    TransactionResponse.builder().id("txn-1").status("APPLIED").build()
            );

            AccountDetails details = AccountDetails.builder()
                    .accountId("acct-001")
                    .balance(200.0)
                    .currency("USD")
                    .transactionCount(5)
                    .createdAt(now)
                    .updatedAt(now)
                    .recentTransactions(transactions)
                    .build();

            assertThat(details.getAccountId()).isEqualTo("acct-001");
            assertThat(details.getBalance()).isEqualTo(200.0);
            assertThat(details.getCurrency()).isEqualTo("USD");
            assertThat(details.getTransactionCount()).isEqualTo(5);
            assertThat(details.getRecentTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("should support empty recent transactions")
        void shouldSupportEmptyRecentTransactions() {
            AccountDetails details = AccountDetails.builder()
                    .accountId("acct-002")
                    .recentTransactions(Collections.emptyList())
                    .build();

            assertThat(details.getRecentTransactions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ErrorResponse DTO")
    class ErrorResponseTests {

        @Test
        @DisplayName("should create ErrorResponse with builder")
        void shouldCreateWithBuilder() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> details = Map.of("field", "amount");

            ErrorResponse response = ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message("Amount must be positive")
                    .timestamp(now)
                    .traceId("trace-001")
                    .details(details)
                    .build();

            assertThat(response.getError()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getMessage()).isEqualTo("Amount must be positive");
            assertThat(response.getTimestamp()).isEqualTo(now);
            assertThat(response.getTraceId()).isEqualTo("trace-001");
            assertThat(response.getDetails()).containsEntry("field", "amount");
        }

        @Test
        @DisplayName("should support null details")
        void shouldSupportNullDetails() {
            ErrorResponse response = ErrorResponse.builder()
                    .error("INTERNAL_SERVER_ERROR")
                    .message("Unexpected error")
                    .details(null)
                    .build();

            assertThat(response.getDetails()).isNull();
        }
    }
}
