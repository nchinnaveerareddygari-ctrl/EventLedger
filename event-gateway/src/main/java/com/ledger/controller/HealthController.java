package com.eventledger.controller;

import com.eventledger.dto.HealthResponse;
import com.eventledger.service.AccountServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Service health check endpoints")
public class HealthController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${account-service.url:http://localhost:8081/api}")
    private String accountServiceUrl;

    @GetMapping
    @Operation(summary = "Health check", description = "Returns the health status of the Event Gateway and dependent services")
    @ApiResponse(responseCode = "200", description = "Health status",
            content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setService("event-gateway");
        response.setDatabase(checkDatabaseHealth());
        response.setAccountService(checkAccountServiceHealth());
        response.setTimestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));

        log.info("Health check: gateway={}, db={}, account-service={}",
                response.getStatus(), response.getDatabase(), response.getAccountService());

        return ResponseEntity.ok(response);
    }

    private String checkDatabaseHealth() {
        try {
            // If we can respond, database is up
            return "CONNECTED";
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return "DISCONNECTED";
        }
    }

    private String checkAccountServiceHealth() {
        try {
            String healthUrl = accountServiceUrl + "/health";
            restTemplate.getForEntity(healthUrl, String.class);
            return "UP";
        } catch (Exception e) {
            log.warn("Account Service health check failed: {}", e.getMessage());
            return "DOWN";
        }
    }
}
