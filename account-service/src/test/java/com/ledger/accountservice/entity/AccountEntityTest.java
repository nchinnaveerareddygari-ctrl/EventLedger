package com.ledger.accountservice.entity;

import com.eventledger.account.entity.Account;
import com.eventledger.account.entity.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountEntityTest {

    @Test
    @DisplayName("should create Account using builder")
    void shouldCreateAccountUsingBuilder() {
        LocalDateTime now = LocalDateTime.now();

        Account account = Account.builder()
                .accountId("acct-001")
                .balance(500.0)
                .currency("USD")
                .transactionCount(10)
                .createdAt(now)
                .updatedAt(now)
                .lastEventTimestamp(now)
                .build();

        assertThat(account.getAccountId()).isEqualTo("acct-001");
        assertThat(account.getBalance()).isEqualTo(500.0);
        assertThat(account.getCurrency()).isEqualTo("USD");
        assertThat(account.getTransactionCount()).isEqualTo(10);
        assertThat(account.getCreatedAt()).isEqualTo(now);
        assertThat(account.getUpdatedAt()).isEqualTo(now);
        assertThat(account.getLastEventTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("should create Account using no-args constructor and setters")
    void shouldCreateAccountUsingSetters() {
        Account account = new Account();
        account.setAccountId("acct-002");
        account.setBalance(0.0);
        account.setCurrency("EUR");
        account.setTransactionCount(0);

        assertThat(account.getAccountId()).isEqualTo("acct-002");
        assertThat(account.getBalance()).isEqualTo(0.0);
        assertThat(account.getCurrency()).isEqualTo("EUR");
        assertThat(account.getTransactionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should set createdAt and updatedAt on PrePersist")
    void shouldSetTimestampsOnPrePersist() {
        Account account = new Account();
        account.setAccountId("acct-003");
        account.setBalance(0.0);
        account.setCurrency("USD");

        account.onCreate();

        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should not override createdAt if already set on PrePersist")
    void shouldNotOverrideCreatedAtOnPrePersist() {
        LocalDateTime existingTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        Account account = new Account();
        account.setCreatedAt(existingTime);
        account.setUpdatedAt(existingTime);

        account.onCreate();

        assertThat(account.getCreatedAt()).isEqualTo(existingTime);
        assertThat(account.getUpdatedAt()).isEqualTo(existingTime);
    }

    @Test
    @DisplayName("should update updatedAt on PreUpdate")
    void shouldUpdateUpdatedAtOnPreUpdate() {
        LocalDateTime oldTime = LocalDateTime.of(2023, 1, 1, 0, 0);
        Account account = new Account();
        account.setUpdatedAt(oldTime);

        account.onUpdate();

        assertThat(account.getUpdatedAt()).isAfter(oldTime);
    }

    @Test
    @DisplayName("should support transactions list")
    void shouldSupportTransactionsList() {
        Account account = Account.builder()
                .accountId("acct-004")
                .balance(0.0)
                .currency("USD")
                .transactions(new ArrayList<>())
                .build();

        assertThat(account.getTransactions()).isNotNull();
        assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    @DisplayName("should implement equals and hashCode via Lombok @Data")
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        Account account1 = Account.builder().accountId("acct-001").balance(100.0).currency("USD").createdAt(now).updatedAt(now).build();
        Account account2 = Account.builder().accountId("acct-001").balance(100.0).currency("USD").createdAt(now).updatedAt(now).build();

        assertThat(account1).isEqualTo(account2);
        assertThat(account1.hashCode()).isEqualTo(account2.hashCode());
    }

    @Test
    @DisplayName("should implement toString via Lombok @Data")
    void shouldImplementToString() {
        Account account = Account.builder().accountId("acct-001").balance(100.0).currency("USD").build();

        String toString = account.toString();
        assertThat(toString).contains("acct-001");
        assertThat(toString).contains("100.0");
        assertThat(toString).contains("USD");
    }
}
