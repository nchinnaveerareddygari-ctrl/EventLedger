package com.ledger.accountservice.service;

import com.eventledger.account.dto.request.TransactionRequest;
import com.eventledger.account.dto.response.AccountDetails;
import com.eventledger.account.dto.response.BalanceResponse;
import com.eventledger.account.dto.response.TransactionResponse;
import com.eventledger.account.entity.Account;
import com.eventledger.account.entity.Transaction;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import com.eventledger.account.service.IAccountService;
import com.eventledger.account.service.impl.AccountService;
import com.eventledger.account.service.validation.TransactionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionValidator transactionValidator;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AccountService accountService;

    private static final String ACCOUNT_ID = "acct-123";
    private static final String TRACE_ID = "trace-001";
    private static final String EVENT_ID = "evt-001";

    private Account testAccount;
    private TransactionRequest testRequest;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .accountId(ACCOUNT_ID)
                .balance(100.0)
                .currency("USD")
                .transactionCount(1)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .lastEventTimestamp(LocalDateTime.now().minusHours(1))
                .build();

        testRequest = new TransactionRequest();
        testRequest.setEventId(EVENT_ID);
        testRequest.setAccountId(ACCOUNT_ID);
        testRequest.setType("CREDIT");
        testRequest.setAmount(50.0);
        testRequest.setCurrency("USD");
        testRequest.setEventTimestamp(LocalDateTime.now());

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .eventId(EVENT_ID)
                .accountId(ACCOUNT_ID)
                .type("CREDIT")
                .amount(50.0)
                .currency("USD")
                .eventTimestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .status("APPLIED")
                .build();
    }

    @Nested
    @DisplayName("applyTransaction")
    class ApplyTransaction {

        @Test
        @DisplayName("should apply a new transaction successfully")
        void shouldApplyNewTransaction() {
            when(transactionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(transactionRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            TransactionResponse response = accountService.applyTransaction(ACCOUNT_ID, testRequest, TRACE_ID);

            assertThat(response).isNotNull();
            assertThat(response.getEventId()).isEqualTo(EVENT_ID);
            assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.getType()).isEqualTo("CREDIT");
            assertThat(response.getAmount()).isEqualTo(50.0);
            verify(transactionValidator).validate(testRequest, TRACE_ID);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should return existing transaction for duplicate eventId")
        void shouldReturnDuplicateForExistingEventId() {
            when(transactionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(testTransaction));

            TransactionResponse response = accountService.applyTransaction(ACCOUNT_ID, testRequest, TRACE_ID);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("DUPLICATE");
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should create new account if not found")
        void shouldCreateNewAccountIfNotFound() {
            Account newAccount = Account.builder()
                    .accountId(ACCOUNT_ID)
                    .balance(0.0)
                    .currency("USD")
                    .transactionCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenReturn(newAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(transactionRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));

            TransactionResponse response = accountService.applyTransaction(ACCOUNT_ID, testRequest, TRACE_ID);

            assertThat(response).isNotNull();
            verify(accountRepository, atLeast(2)).save(any(Account.class));
        }

        @Test
        @DisplayName("should mark out-of-order event")
        void shouldMarkOutOfOrderEvent() {
            testRequest.setEventTimestamp(LocalDateTime.now().minusDays(2));

            when(transactionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(transactionRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            TransactionResponse response = accountService.applyTransaction(ACCOUNT_ID, testRequest, TRACE_ID);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("OUT_OF_ORDER");
        }

        @Test
        @DisplayName("should serialize metadata when present")
        void shouldSerializeMetadata() throws Exception {
            Map<String, Object> metadata = Map.of("key", "value");
            testRequest.setMetadata(metadata);

            when(transactionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(transactionRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(objectMapper.writeValueAsString(metadata)).thenReturn("{\"key\":\"value\"}");

            TransactionResponse response = accountService.applyTransaction(ACCOUNT_ID, testRequest, TRACE_ID);

            assertThat(response).isNotNull();
            verify(objectMapper).writeValueAsString(metadata);
        }
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("should return balance for existing account")
        void shouldReturnBalanceForExistingAccount() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));

            BalanceResponse response = accountService.getBalance(ACCOUNT_ID, TRACE_ID);

            assertThat(response).isNotNull();
            assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.getCurrency()).isEqualTo("USD");
            assertThat(response.getBalance()).isEqualTo(50.0);
            assertThat(response.getCalculatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when account doesn't exist")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getBalance(ACCOUNT_ID, TRACE_ID))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("should return zero balance when no transactions")
        void shouldReturnZeroBalanceWhenNoTransactions() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(Collections.emptyList());

            BalanceResponse response = accountService.getBalance(ACCOUNT_ID, TRACE_ID);

            assertThat(response.getBalance()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should calculate balance with credits and debits")
        void shouldCalculateBalanceWithCreditsAndDebits() {
            Transaction credit = Transaction.builder()
                    .type("CREDIT").amount(100.0).build();
            Transaction debit = Transaction.builder()
                    .type("DEBIT").amount(30.0).build();

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(List.of(credit, debit));

            BalanceResponse response = accountService.getBalance(ACCOUNT_ID, TRACE_ID);

            assertThat(response.getBalance()).isEqualTo(70.0);
        }
    }

    @Nested
    @DisplayName("getAccountDetails")
    class GetAccountDetails {

        @Test
        @DisplayName("should return account details for existing account")
        void shouldReturnAccountDetails() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));
            when(transactionRepository.findRecentTransactionsByAccountId(ACCOUNT_ID, 10)).thenReturn(List.of(testTransaction));
            when(transactionRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of(testTransaction));

            AccountDetails details = accountService.getAccountDetails(ACCOUNT_ID, TRACE_ID);

            assertThat(details).isNotNull();
            assertThat(details.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(details.getCurrency()).isEqualTo("USD");
            assertThat(details.getTransactionCount()).isEqualTo(1);
            assertThat(details.getRecentTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when account doesn't exist")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getAccountDetails(ACCOUNT_ID, TRACE_ID))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("should return empty recent transactions when none exist")
        void shouldReturnEmptyRecentTransactions() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findTransactionsByAccountId(ACCOUNT_ID)).thenReturn(Collections.emptyList());
            when(transactionRepository.findRecentTransactionsByAccountId(ACCOUNT_ID, 10)).thenReturn(Collections.emptyList());
            when(transactionRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(Collections.emptyList());

            AccountDetails details = accountService.getAccountDetails(ACCOUNT_ID, TRACE_ID);

            assertThat(details.getRecentTransactions()).isEmpty();
            assertThat(details.getTransactionCount()).isEqualTo(0);
        }
    }
}