package com.eventledger.service;

import com.eventledger.dto.EventRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * Account Service Client with OAuth2 Authentication
 *
 * This service handles all communication with the Account Service
 * It automatically injects OAuth2 Bearer tokens into outgoing requests
 * using Client Credentials flow
 *
 * Includes resilience patterns:
 * - Circuit Breaker: Prevents cascading failures
 * - Retry: Handles transient failures
 * - TimeLimiter: Enforces timeout to prevent hanging requests
 */
@Slf4j
@Service
public class AccountServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Value("${account-service.url:http://localhost:8081/api}")
    private String accountServiceUrl;

    @Value("${oauth2.account-service.client-id:account-service-client}")
    private String clientId;

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String FALLBACK_METHOD = "fallback";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    /**
     * Apply transaction with OAuth2 authentication
     *
     * Resilience patterns:
     * - Circuit Breaker: stops calling service if it fails repeatedly
     * - Retry: retries failed requests with backoff
     * - TimeLimiter: enforces timeout
     *
     * @param request - The event request to process
     * @param traceId - Distributed trace ID
     * @return CompletableFuture for async execution
     */
    @CircuitBreaker(name = "accountService", fallbackMethod = FALLBACK_METHOD)
    @Retry(name = "accountService")
    @TimeLimiter(name = "accountService")
    public CompletableFuture<Void> applyTransaction(EventRequest request, String traceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = accountServiceUrl + "/accounts/" + request.getAccountId() + "/transactions";
                String requestId = generateRequestId();

                // Build headers with trace ID and authentication
                HttpHeaders headers = buildAuthenticatedHeaders(traceId, requestId);

                log.debug("[{}] Building request to Account Service - URL: {}, RequestID: {}",
                        traceId, url, requestId);

                // Create entity with event request and headers
                org.springframework.http.HttpEntity<EventRequest> entity =
                        new org.springframework.http.HttpEntity<>(request, headers);

                log.info("[{}] Calling Account Service: POST {} with OAuth2 Bearer token [RequestID: {}]",
                        traceId, url, requestId);

                // Make the call to Account Service with OAuth2 authentication
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.error("[{}] Account Service returned error status: {} [RequestID: {}]",
                            traceId, response.getStatusCode(), requestId);
                    throw new RuntimeException(
                            "Account Service returned status: " + response.getStatusCode());
                }

                log.info("[{}] Account Service call successful [RequestID: {}]", traceId, requestId);

            } catch (HttpClientErrorException e) {
                // 4xx errors - client's fault
                log.error("[{}] Client error calling Account Service - Status: {}, Message: {}",
                        traceId, e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException(
                        "Account Service client error: " + e.getStatusCode() + " - " + e.getMessage(), e);

            } catch (HttpServerErrorException e) {
                // 5xx errors - server's fault
                log.error("[{}] Server error calling Account Service - Status: {}, Message: {}",
                        traceId, e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException(
                        "Account Service server error: " + e.getStatusCode() + " - " + e.getMessage(), e);

            } catch (RestClientException e) {
                log.error("[{}] Failed to call Account Service due to connection error", traceId, e);
                throw new RuntimeException("Account Service call failed: " + e.getMessage(), e);

            } catch (Exception e) {
                log.error("[{}] Unexpected error calling Account Service", traceId, e);
                throw new RuntimeException("Unexpected error calling Account Service: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get account health status with OAuth2 authentication
     * Used for health checks and monitoring
     */
    public boolean checkAccountServiceHealth(String traceId) {
        try {
            String url = accountServiceUrl + "/health";
            HttpHeaders headers = buildAuthenticatedHeaders(traceId, generateRequestId());

            org.springframework.http.HttpEntity<Void> entity =
                    new org.springframework.http.HttpEntity<>(null, headers);

            log.debug("[{}] Checking Account Service health with OAuth2", traceId);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            log.info("[{}] Account Service health check - Status: {} [Healthy: {}]",
                    traceId, response.getStatusCode(), isHealthy);

            return isHealthy;

        } catch (Exception e) {
            log.warn("[{}] Account Service health check failed", traceId, e);
            return false;
        }
    }

    /**
     * Build HTTP headers with OAuth2 Bearer token and trace information
     */
    private HttpHeaders buildAuthenticatedHeaders(String traceId, String requestId) {
        HttpHeaders headers = new HttpHeaders();

        // Add trace headers for distributed tracing
        headers.set(TRACE_ID_HEADER, traceId);
        headers.set(REQUEST_ID_HEADER, requestId);

        // Add Content-Type header
        headers.set("Content-Type", "application/json");

        // Add Bearer token from OAuth2 client credentials
        String bearerToken = getOAuth2BearerToken();
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
            log.debug("[{}] OAuth2 Bearer token added to headers", traceId);
        } else {
            log.warn("[{}] No OAuth2 Bearer token available - calling without authentication", traceId);
        }

        return headers;
    }

    /**
     * Get OAuth2 Bearer token using Client Credentials flow
     */
    private String getOAuth2BearerToken() {
        try {
            if (authorizedClientManager == null) {
                log.warn("OAuth2AuthorizedClientManager not available, authentication disabled");
                return null;
            }

            // Create authentication request for client credentials flow
            OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                    null,
                    null,
                    clientId);

            // Get authorized client - handles token acquisition and refresh
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(
                    request -> request
                            .setClientRegistrationId(clientId)
                            .setPrincipal(authentication));

            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String token = authorizedClient.getAccessToken().getTokenValue();
                log.debug("OAuth2 Bearer token acquired successfully");
                return token;
            }

            log.warn("Failed to acquire OAuth2 Bearer token");
            return null;

        } catch (Exception e) {
            log.error("Error acquiring OAuth2 Bearer token", e);
            return null;
        }
    }

    /**
     * Generate unique request ID for tracking
     */
    private String generateRequestId() {
        return "req-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Fallback method when Account Service is unavailable
     * Called by Circuit Breaker when service fails repeatedly
     */
    public CompletableFuture<Void> fallback(EventRequest request, String traceId, Exception ex) {
        log.error("[{}] Circuit breaker activated for Account Service. Service temporarily unavailable.",
                traceId, ex);
        return CompletableFuture.failedFuture(
                new RuntimeException(
                        "Account Service is temporarily unavailable. Please retry later. " +
                                "Reason: " + ex.getMessage(),
                        ex)
        );
    }
}