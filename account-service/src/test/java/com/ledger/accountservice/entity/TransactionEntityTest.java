package com.ledger.accountservice.entity;

import com.eventledger.account.entity.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntityTest {

    @Test
    @DisplayName("should create Transaction using builder")
    void shouldCreateTransactionUsingBuilder() {
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .id("txn-001")
                .eventId("evt-001")
                .accountId("acct-001")
                .type("CREDIT")
                .amount(100.0)
                .currency("USD")
                .eventTimestamp(now)
                .createdAt(now)
                .status("APPLIED")
                .metadata("{\"key\":\"value\"}")
                .build();

        assertThat(transaction.getId()).isEqualTo("txn-001");
        assertThat(transaction.getEventId()).isEqualTo("evt-001");
        assertThat(transaction.getAccountId()).isEqualTo("acct-001");
        assertThat(transaction.getType()).isEqualTo("CREDIT");
        assertThat(transaction.getAmount()).isEqualTo(100.0);
        assertThat(transaction.getCurrency()).isEqualTo("USD");
        assertThat(transaction.getEventTimestamp()).isEqualTo(now);
        assertThat(transaction.getCreatedAt()).isEqualTo(now);
        assertThat(transaction.getStatus()).isEqualTo("APPLIED");
        assertThat(transaction.getMetadata()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("should create Transaction using no-args constructor and setters")
    void shouldCreateTransactionUsingSetters() {
        Transaction transaction = new Transaction();
        transaction.setId("txn-002");
        transaction.setEventId("evt-002");
        transaction.setAccountId("acct-002");
        transaction.setType("DEBIT");
        transaction.setAmount(50.0);
        transaction.setCurrency("EUR");
        transaction.setStatus("APPLIED");

        assertThat(transaction.getId()).isEqualTo("txn-002");
        assertThat(transaction.getType()).isEqualTo("DEBIT");
        assertThat(transaction.getAmount()).isEqualTo(50.0);
        assertThat(transaction.getCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("should set createdAt and status on PrePersist")
    void shouldSetDefaultsOnPrePersist() {
        Transaction transaction = new Transaction();
        transaction.setId("txn-003");

        transaction.onCreate();

        assertThat(transaction.getCreatedAt()).isNotNull();
        assertThat(transaction.getStatus()).isEqualTo("APPLIED");
    }

    @Test
    @DisplayName("should not override createdAt if already set on PrePersist")
    void shouldNotOverrideCreatedAtOnPrePersist() {
        LocalDateTime existingTime = LocalDateTime.of(2023, 6, 15, 12, 0);
        Transaction transaction = new Transaction();
        transaction.setCreatedAt(existingTime);
        transaction.setStatus("DUPLICATE");

        transaction.onCreate();

        assertThat(transaction.getCreatedAt()).isEqualTo(existingTime);
        assertThat(transaction.getStatus()).isEqualTo("DUPLICATE");
    }

    @Test
    @DisplayName("should support DEBIT type")
    void shouldSupportDebitType() {
        Transaction transaction = Transaction.builder()
                .id("txn-004")
                .type("DEBIT")
                .amount(25.0)
                .build();

        assertThat(transaction.getType()).isEqualTo("DEBIT");
    }

    @Test
    @DisplayName("should support DUPLICATE status")
    void shouldSupportDuplicateStatus() {
        Transaction transaction = Transaction.builder()
                .id("txn-005")
                .status("DUPLICATE")
                .build();

        assertThat(transaction.getStatus()).isEqualTo("DUPLICATE");
    }

    @Test
    @DisplayName("should support OUT_OF_ORDER status")
    void shouldSupportOutOfOrderStatus() {
        Transaction transaction = Transaction.builder()
                .id("txn-006")
                .status("OUT_OF_ORDER")
                .build();

        assertThat(transaction.getStatus()).isEqualTo("OUT_OF_ORDER");
    }

    @Test
    @DisplayName("should support null metadata")
    void shouldSupportNullMetadata() {
        Transaction transaction = Transaction.builder()
                .id("txn-007")
                .metadata(null)
                .build();

        assertThat(transaction.getMetadata()).isNull();
    }

    @Test
    @DisplayName("should implement equals and hashCode via Lombok @Data")
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        Transaction txn1 = Transaction.builder().id("txn-001").eventId("evt-001").accountId("acct-001")
                .type("CREDIT").amount(100.0).currency("USD").eventTimestamp(now).createdAt(now).status("APPLIED").build();
        Transaction txn2 = Transaction.builder().id("txn-001").eventId("evt-001").accountId("acct-001")
                .type("CREDIT").amount(100.0).currency("USD").eventTimestamp(now).createdAt(now).status("APPLIED").build();

        assertThat(txn1).isEqualTo(txn2);
        assertThat(txn1.hashCode()).isEqualTo(txn2.hashCode());
    }

    @Test
    @DisplayName("should implement toString via Lombok @Data")
    void shouldImplementToString() {
        Transaction transaction = Transaction.builder().id("txn-001").eventId("evt-001").type("CREDIT").build();

        String toString = transaction.toString();
        assertThat(toString).contains("txn-001");
        assertThat(toString).contains("evt-001");
        assertThat(toString).contains("CREDIT");
    }
}
