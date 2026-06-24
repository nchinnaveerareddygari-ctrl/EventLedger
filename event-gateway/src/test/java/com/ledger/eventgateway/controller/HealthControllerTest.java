package com.eventledger.controller;

import com.eventledger.dto.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Health Controller Tests")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    // ==================== Health Check Tests ====================

    @Test
    @DisplayName("Should return UP status when all services are healthy")
    void testHealthCheckAllServicesUp() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("event-gateway"))
                .andExpect(jsonPath("$.database").value("CONNECTED"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should return service name in health response")
    void testHealthCheckServiceName() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("event-gateway"));
    }

    @Test
    @DisplayName("Should include database health status")
    void testHealthCheckDatabaseStatus() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").exists())
                .andExpect(jsonPath("$.database", anyOf(equalTo("CONNECTED"), equalTo("DISCONNECTED"))));
    }

    @Test
    @DisplayName("Should include account service health status")
    void testHealthCheckAccountServiceStatus() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountService").exists())
                .andExpect(jsonPath("$.accountService", anyOf(equalTo("UP"), equalTo("DOWN"))));
    }

    @Test
    @DisplayName("Should include valid timestamp in ISO format")
    void testHealthCheckTimestampFormat() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*")));
    }

    @Test
    @DisplayName("Should return proper JSON content type")
    void testHealthCheckContentType() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should handle rapid successive health checks")
    void testHealthCheckRapidRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle missing health endpoint gracefully")
    void testHealthCheckInvalidEndpoint() throws Exception {
        mockMvc.perform(get("/health/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should only respond to GET requests")
    void testHealthCheckPostNotAllowed() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    // ==================== Response Structure Tests ====================

    @Test
    @DisplayName("Should contain all required fields in health response")
    void testHealthCheckAllRequiredFields() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.service").exists())
                .andExpect(jsonPath("$.database").exists())
                .andExpect(jsonPath("$.accountService").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should not include null values in response")
    void testHealthCheckNoNullValues() throws Exception {
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.service").isNotEmpty())
                .andExpect(jsonPath("$.database").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}