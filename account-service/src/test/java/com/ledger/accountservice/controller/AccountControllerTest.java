package com.ledger.accountservice.controller;

import com.eventledger.account.controller.AccountController;
import com.eventledger.account.dto.request.TransactionRequest;
import com.eventledger.account.dto.response.AccountDetails;
import com.eventledger.account.dto.response.BalanceResponse;
import com.eventledger.account.dto.response.TransactionResponse;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.exception.GlobalExceptionHandler;
import com.eventledger.account.service.IAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private IAccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String ACCOUNT_ID = "acct-123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("GET /api/v1/health")
    class HealthEndpoint {

        @Test
        @DisplayName("should return 200 with health status UP")
        void shouldReturnHealthStatus() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("account-service"));
        }

        @Test
        @DisplayName("should use provided trace ID header")
        void shouldUseProvidedTraceId() throws Exception {
            mockMvc.perform(get("/api/v1/health")
                            .header("X-Trace-ID", "my-trace-id"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/{accountId}")
    class GetAccountEndpoint {

        @Test
        @DisplayName("should return 200 with account details")
        void shouldReturnAccountDetails() throws Exception {
            AccountDetails details = AccountDetails.builder()
                    .accountId(ACCOUNT_ID)
                    .balance(100.0)
                    .currency("USD")
                    .transactionCount(5)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .recentTransactions(Collections.emptyList())
                    .build();

            when(accountService.getAccountDetails(eq(ACCOUNT_ID), anyString())).thenReturn(details);

            mockMvc.perform(get("/api/v1/accounts/{accountId}", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$.balance").value(100.0))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.transactionCount").value(5));
        }

        @Test
        @DisplayName("should return 404 when account not found")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            when(accountService.getAccountDetails(eq(ACCOUNT_ID), anyString()))
                    .thenThrow(new AccountNotFoundException(ACCOUNT_ID));

            mockMvc.perform(get("/api/v1/accounts/{accountId}", ACCOUNT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/{accountId}/balance")
    class GetBalanceEndpoint {

        @Test
        @DisplayName("should return 200 with balance")
        void shouldReturnBalance() throws Exception {
            BalanceResponse balance = BalanceResponse.builder()
                    .accountId(ACCOUNT_ID)
                    .balance(250.50)
                    .currency("USD")
                    .calculatedAt(LocalDateTime.now())
                    .build();

            when(accountService.getBalance(eq(ACCOUNT_ID), anyString())).thenReturn(balance);

            mockMvc.perform(get("/api/v1/accounts/{accountId}/balance", ACCOUNT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                    .andExpect(jsonPath("$.balance").value(250.50))
                    .andExpect(jsonPath("$.currency").value("USD"));
        }

        @Test
        @DisplayName("should return 404 when account not found")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            when(accountService.getBalance(eq(ACCOUNT_ID), anyString()))
                    .thenThrow(new AccountNotFoundException(ACCOUNT_ID));

            mockMvc.perform(get("/api/v1/accounts/{accountId}/balance", ACCOUNT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/accounts/{accountId}/transactions")
    class ApplyTransactionEndpoint {

        @Test
        @DisplayName("should return 201 when transaction applied successfully")
        void shouldReturn201WhenTransactionApplied() throws Exception {
            TransactionResponse response = TransactionResponse.builder()
                    .id("txn-001")
                    .eventId("evt-001")
                    .accountId(ACCOUNT_ID)
                    .type("CREDIT")
                    .amount(50.0)
                    .currency("USD")
                    .status("APPLIED")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(accountService.applyTransaction(eq(ACCOUNT_ID), any(TransactionRequest.class), anyString()))
                    .thenReturn(response);

            String requestBody = """
                    {
                        "eventId": "evt-001",
                        "accountId": "acct-123",
                        "type": "CREDIT",
                        "amount": 50.0,
                        "currency": "USD",
                        "eventTimestamp": "2024-01-15T10:30:00"
                    }
                    """;

            mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("txn-001"))
                    .andExpect(jsonPath("$.status").value("APPLIED"));
        }

        @Test
        @DisplayName("should return 404 when account not found during transaction")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            when(accountService.applyTransaction(eq(ACCOUNT_ID), any(TransactionRequest.class), anyString()))
                    .thenThrow(new AccountNotFoundException(ACCOUNT_ID));

            String requestBody = """
                    {
                        "eventId": "evt-001",
                        "accountId": "acct-123",
                        "type": "CREDIT",
                        "amount": 50.0,
                        "currency": "USD",
                        "eventTimestamp": "2024-01-15T10:30:00"
                    }
                    """;

            mockMvc.perform(post("/api/v1/accounts/{accountId}/transactions", ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }
}


