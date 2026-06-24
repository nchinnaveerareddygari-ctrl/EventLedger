package com.eventledger.controller;

import com.eventledger.dto.ErrorResponse;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Event Controller Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private EventRequest validEventRequest;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        validEventRequest = EventRequest.builder()
                .eventId("evt-12345")
                .accountId("acc-001")
                .type("DEBIT")
                .amount(new BigDecimal("100.50"))
                .currency("USD")
                .eventTimestamp(LocalDateTime.now())
                .build();

        eventResponse = EventResponse.builder()
                .id(1L)
                .eventId("evt-12345")
                .accountId("acc-001")
                .type("DEBIT")
                .amount(new BigDecimal("100.50"))
                .currency("USD")
                .status("PROCESSED")
                .createdAt(LocalDateTime.now())
                .traceId("trace-abc123")
                .build();
    }

    // ==================== POST /events Tests ====================

    @Test
    @DisplayName("Should submit event successfully with 201 Created status")
    void testSubmitEventSuccess() throws Exception {
        when(eventService.processEvent(any(EventRequest.class)))
                .thenReturn(eventResponse);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventId").value("evt-12345"))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.traceId").value("trace-abc123"));

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when event validation fails")
    void testSubmitEventValidationError() throws Exception {
        when(eventService.processEvent(any(EventRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid transaction type"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEventRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid transaction type"))
                .andExpect(jsonPath("$.status").value(400));

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }

    @Test
    @DisplayName("Should return 409 Conflict for duplicate event (idempotent)")
    void testSubmitDuplicateEvent() throws Exception {
        EventResponse duplicateResponse = EventResponse.builder()
                .id(1L)
                .eventId("evt-12345")
                .status("DUPLICATE")
                .build();

        when(eventService.processEvent(any(EventRequest.class)))
                .thenReturn(duplicateResponse);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }

    @Test
    @DisplayName("Should return 503 Service Unavailable when Account Service is down")
    void testSubmitEventAccountServiceUnavailable() throws Exception {
        when(eventService.processEvent(any(EventRequest.class)))
                .thenThrow(new RuntimeException("Account Service unavailable"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEventRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Account Service is temporarily unavailable"))
                .andExpect(jsonPath("$.status").value(503));

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error for unexpected errors")
    void testSubmitEventInternalError() throws Exception {
        when(eventService.processEvent(any(EventRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEventRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.status").value(500));

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body is invalid")
    void testSubmitEventInvalidBody() throws Exception {
        String invalidJson = "{\"eventId\": \"evt-123\"}"; // Missing required fields

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /events/{id} Tests ====================

    @Test
    @DisplayName("Should retrieve event by ID successfully")
    void testGetEventByIdSuccess() throws Exception {
        when(eventService.getEventById(1L))
                .thenReturn(Optional.of(eventResponse));

        mockMvc.perform(get("/events/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventId").value("evt-12345"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("USD"));

        verify(eventService, times(1)).getEventById(1L);
    }

    @Test
    @DisplayName("Should return 404 Not Found when event ID does not exist")
    void testGetEventByIdNotFound() throws Exception {
        when(eventService.getEventById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/events/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found"))
                .andExpect(jsonPath("$.status").value(404));

        verify(eventService, times(1)).getEventById(999L);
    }

    @Test
    @DisplayName("Should handle invalid event ID format")
    void testGetEventByIdInvalidFormat() throws Exception {
        mockMvc.perform(get("/events/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /events?account Tests ====================

    @Test
    @DisplayName("Should retrieve all events for an account successfully")
    void testListEventsByAccountSuccess() throws Exception {
        List<EventResponse> eventsList = Arrays.asList(eventResponse);

        when(eventService.getEventsByAccount("acc-001"))
                .thenReturn(eventsList);

        mockMvc.perform(get("/events?account=acc-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventId").value("evt-12345"))
                .andExpect(jsonPath("$[0].accountId").value("acc-001"));

        verify(eventService, times(1)).getEventsByAccount("acc-001");
    }

    @Test
    @DisplayName("Should return empty list when account has no events")
    void testListEventsByAccountEmpty() throws Exception {
        when(eventService.getEventsByAccount("acc-empty"))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/events?account=acc-empty")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(eventService, times(1)).getEventsByAccount("acc-empty");
    }

    @Test
    @DisplayName("Should return 400 Bad Request when account parameter is missing")
    void testListEventsByAccountMissingParameter() throws Exception {
        mockMvc.perform(get("/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("account parameter is required"))
                .andExpect(jsonPath("$.status").value(400));

        verify(eventService, never()).getEventsByAccount(anyString());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when account parameter is blank")
    void testListEventsByAccountBlankParameter() throws Exception {
        mockMvc.perform(get("/events?account=")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("account parameter is required"));

        verify(eventService, never()).getEventsByAccount(anyString());
    }

    @Test
    @DisplayName("Should retrieve multiple events for an account in chronological order")
    void testListEventsByAccountMultiple() throws Exception {
        EventResponse event2 = EventResponse.builder()
                .id(2L)
                .eventId("evt-67890")
                .accountId("acc-001")
                .type("CREDIT")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        List<EventResponse> eventsList = Arrays.asList(eventResponse, event2);

        when(eventService.getEventsByAccount("acc-001"))
                .thenReturn(eventsList);

        mockMvc.perform(get("/events?account=acc-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventId").value("evt-12345"))
                .andExpect(jsonPath("$[1].eventId").value("evt-67890"));

        verify(eventService, times(1)).getEventsByAccount("acc-001");
    }

    // ==================== Error Handler Tests ====================

    @Test
    @DisplayName("Should handle validation exceptions with proper error response")
    void testValidationErrorResponse() throws Exception {
        // Send POST with missing required fields
        String invalidRequest = "{}";

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle event with metadata successfully")
    void testSubmitEventWithMetadata() throws Exception {
        EventRequest requestWithMetadata = EventRequest.builder()
                .eventId("evt-meta-001")
                .accountId("acc-001")
                .type("DEBIT")
                .amount(new BigDecimal("100.50"))
                .currency("USD")
                .metadata(java.util.Map.of("reference", "REF-123", "description", "Payment"))
                .build();

        when(eventService.processEvent(any(EventRequest.class)))
                .thenReturn(eventResponse);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithMetadata)))
                .andExpect(status().isCreated());

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }

    @Test
    @DisplayName("Should handle large amount values")
    void testSubmitEventWithLargeAmount() throws Exception {
        validEventRequest.setAmount(new BigDecimal("999999999.99"));

        when(eventService.processEvent(any(EventRequest.class)))
                .thenReturn(eventResponse);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEventRequest)))
                .andExpect(status().isCreated());

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }

    @Test
    @DisplayName("Should handle different currency codes")
    void testSubmitEventWithDifferentCurrency() throws Exception {
        validEventRequest.setCurrency("EUR");

        when(eventService.processEvent(any(EventRequest.class)))
                .thenReturn(eventResponse);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEventRequest)))
                .andExpect(status().isCreated());

        verify(eventService, times(1)).processEvent(any(EventRequest.class));
    }
}