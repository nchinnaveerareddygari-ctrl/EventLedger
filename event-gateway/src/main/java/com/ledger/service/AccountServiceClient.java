package com.eventledger.service;

import com.eventledger.dto.EventRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AccountServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${account-service.url:http://localhost:8081/api}")
    private String accountServiceUrl;

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String FALLBACK_METHOD = "fallback";

    /**
     * Apply transaction with resilience patterns:
     * - Circuit Breaker: stops calling service if it fails repeatedly
     * - Retry: retries failed requests with backoff
     * - TimeLimiter: enforces timeout
     */
    @CircuitBreaker(name = "accountService", fallbackMethod = FALLBACK_METHOD)
    @Retry(name = "accountService")
    @TimeLimiter(name = "accountService")
    public CompletableFuture<Void> applyTransaction(EventRequest request, String traceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = accountServiceUrl + "/accounts/" + request.getAccountId() + "/transactions";

                HttpHeaders headers = new HttpHeaders();
                headers.set(TRACE_ID_HEADER, traceId);
                headers.set("Content-Type", "application/json");

                HttpEntity<EventRequest> entity = new HttpEntity<>(request, headers);

                log.info("Calling Account Service: POST {} [traceId: {}]", url, traceId);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Account Service returned status: " + response.getStatusCode());
                }

                log.info("Account Service response successful [traceId: {}]", traceId);
            } catch (RestClientException e) {
                log.error("Failed to call Account Service [traceId: {}]", traceId, e);
                throw new RuntimeException("Account Service call failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Fallback method when Account Service is unavailable
     */
    public CompletableFuture<Void> fallback(EventRequest request, String traceId, Exception ex) {
        log.error("Circuit breaker activated for Account Service. Returning error to client [traceId: {}]", traceId, ex);
        return CompletableFuture.failedFuture(
                new RuntimeException("Account Service is temporarily unavailable. Please retry later.", ex)
        );
    }
}
