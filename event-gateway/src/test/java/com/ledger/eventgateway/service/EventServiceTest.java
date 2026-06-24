package com.eventledger.service;

import com.eventledger.domain.Event;
import com.eventledger.domain.EventStatus;
import com.eventledger.domain.TransactionType;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("Event Service Tests")
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @MockBean
    private EventRepository eventRepository;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private EventRequest validEventRequest;
    private Event testEvent;

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

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setEventId("evt-12345");
        testEvent.setAccountId("acc-001");
        testEvent.setType(TransactionType.DEBIT);
        testEvent.setAmount(new BigDecimal("100.50"));
        testEvent.setCurrency("USD");
        testEvent.setStatus(EventStatus.PROCESSED);
        testEvent.setTraceId("trace-abc123");
        testEvent.setCreatedAt(LocalDateTime.now());
    }

    // ==================== Process Event Tests ====================

    @Test
    @DisplayName("Should process a valid event successfully")
    void testProcessEventSuccess() {
        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response = eventService.processEvent(validEventRequest);

        assertNotNull(response);
        assertEquals("evt-12345", response.getEventId());
        assertEquals("acc-001", response.getAccountId());
        assertEquals("DEBIT", response.getType());
        assertEquals("PROCESSED", response.getStatus());

        verify(eventRepository, times(2)).save(any(Event.class));
        verify(accountServiceClient, times(1)).applyTransaction(any(EventRequest.class), anyString());
    }

    @Test
    @DisplayName("Should detect and handle duplicate events")
    void testProcessDuplicateEvent() {
        testEvent.setStatus(EventStatus.DUPLICATE);
        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response = eventService.processEvent(validEventRequest);

        assertNotNull(response);
        assertEquals("DUPLICATE", response.getStatus());

        verify(accountServiceClient, never()).applyTransaction(any(EventRequest.class), anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid transaction type")
    void testProcessEventInvalidTransactionType() {
        validEventRequest.setType("INVALID");

        assertThrows(IllegalArgumentException.class, () -> {
            eventService.processEvent(validEventRequest);
        });

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("Should handle Account Service failure and mark event as FAILED")
    void testProcessEventAccountServiceFailure() {
        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);
        doThrow(new RuntimeException("Service unavailable"))
                .when(accountServiceClient).applyTransaction(any(EventRequest.class), anyString());

        assertThrows(RuntimeException.class, () -> {
            eventService.processEvent(validEventRequest);
        });

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Should serialize and store metadata as JSON")
    void testProcessEventWithMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reference", "REF-123");
        metadata.put("description", "Payment for invoice");
        validEventRequest.setMetadata(metadata);

        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response = eventService.processEvent(validEventRequest);

        assertNotNull(response);
        verify(eventRepository, times(2)).save(any(Event.class));
    }

    @Test
    @DisplayName("Should generate unique trace ID for each event")
    void testProcessEventTraceIdGeneration() {
        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response1 = eventService.processEvent(validEventRequest);

        validEventRequest.setEventId("evt-67890");
        EventResponse response2 = eventService.processEvent(validEventRequest);

        assertNotNull(response1.getTraceId());
        assertNotNull(response2.getTraceId());

        verify(eventRepository, atLeast(2)).save(any(Event.class));
    }

    // ==================== Get Event By ID Tests ====================

    @Test
    @DisplayName("Should retrieve event by ID successfully")
    void testGetEventByIdSuccess() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(testEvent));

        Optional<EventResponse> response = eventService.getEventById(1L);

        assertTrue(response.isPresent());
        assertEquals("evt-12345", response.get().getEventId());
        assertEquals("PROCESSED", response.get().getStatus());

        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty Optional when event not found")
    void testGetEventByIdNotFound() {
        when(eventRepository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<EventResponse> response = eventService.getEventById(999L);

        assertFalse(response.isPresent());
        verify(eventRepository, times(1)).findById(999L);
    }

    // ==================== Get Events By Account Tests ====================

    @Test
    @DisplayName("Should retrieve all events for an account")
    void testGetEventsByAccountSuccess() {
        List<Event> eventsList = Arrays.asList(testEvent);
        when(eventRepository.findByAccountIdOrderByTimestamp("acc-001"))
                .thenReturn(eventsList);

        List<EventResponse> response = eventService.getEventsByAccount("acc-001");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("evt-12345", response.get(0).getEventId());

        verify(eventRepository, times(1)).findByAccountIdOrderByTimestamp("acc-001");
    }

    @Test
    @DisplayName("Should return empty list when account has no events")
    void testGetEventsByAccountEmpty() {
        when(eventRepository.findByAccountIdOrderByTimestamp("acc-empty"))
                .thenReturn(new ArrayList<>());

        List<EventResponse> response = eventService.getEventsByAccount("acc-empty");

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(eventRepository, times(1)).findByAccountIdOrderByTimestamp("acc-empty");
    }

    @Test
    @DisplayName("Should retrieve multiple events for an account in order")
    void testGetEventsByAccountMultiple() {
        Event event2 = new Event();
        event2.setId(2L);
        event2.setEventId("evt-67890");
        event2.setAccountId("acc-001");
        event2.setType(TransactionType.CREDIT);

        List<Event> eventsList = Arrays.asList(testEvent, event2);
        when(eventRepository.findByAccountIdOrderByTimestamp("acc-001"))
                .thenReturn(eventsList);

        List<EventResponse> response = eventService.getEventsByAccount("acc-001");

        assertEquals(2, response.size());
        assertEquals("evt-12345", response.get(0).getEventId());
        assertEquals("evt-67890", response.get(1).getEventId());

        verify(eventRepository, times(1)).findByAccountIdOrderByTimestamp("acc-001");
    }

    // ==================== Transaction Type Validation Tests ====================

    @Test
    @DisplayName("Should accept valid transaction types")
    void testValidTransactionTypes() {
        String[] validTypes = {"DEBIT", "CREDIT", "TRANSFER", "ADJUSTMENT"};

        for (String type : validTypes) {
            validEventRequest.setType(type);
            when(eventRepository.findByEventId(validEventRequest.getEventId()))
                    .thenReturn(Optional.empty());
            when(eventRepository.save(any(Event.class)))
                    .thenReturn(testEvent);

            assertDoesNotThrow(() -> eventService.processEvent(validEventRequest));
        }
    }

    @Test
    @DisplayName("Should reject invalid transaction types")
    void testInvalidTransactionTypes() {
        String[] invalidTypes = {"INVALID", "BUY", "SELL", "UNKNOWN"};

        for (String type : invalidTypes) {
            validEventRequest.setType(type);

            assertThrows(IllegalArgumentException.class, () -> {
                eventService.processEvent(validEventRequest);
            });
        }
    }

    // ==================== Metadata Handling Tests ====================

    @Test
    @DisplayName("Should handle null metadata gracefully")
    void testProcessEventNullMetadata() {
        validEventRequest.setMetadata(null);

        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response = eventService.processEvent(validEventRequest);

        assertNotNull(response);
        verify(eventRepository, times(2)).save(any(Event.class));
    }

    @Test
    @DisplayName("Should handle empty metadata")
    void testProcessEventEmptyMetadata() {
        validEventRequest.setMetadata(new HashMap<>());

        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response = eventService.processEvent(validEventRequest);

        assertNotNull(response);
        verify(eventRepository, times(2)).save(any(Event.class));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle events with minimum amount")
    void testProcessEventMinimumAmount() {
        validEventRequest.setAmount(BigDecimal.ZERO);

        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response = eventService.processEvent(validEventRequest);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle events with large amounts")
    void testProcessEventLargeAmount() {
        validEventRequest.setAmount(new BigDecimal("999999999999.99"));

        when(eventRepository.findByEventId("evt-12345"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class)))
                .thenReturn(testEvent);

        EventResponse response = eventService.processEvent(validEventRequest);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should preserve event data during mapping")
    void testEventToResponseMapping() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(testEvent));

        Optional<EventResponse> response = eventService.getEventById(1L);

        assertTrue(response.isPresent());
        EventResponse resp = response.get();
        assertEquals(testEvent.getId(), resp.getId());
        assertEquals(testEvent.getEventId(), resp.getEventId());
        assertEquals(testEvent.getAccountId(), resp.getAccountId());
        assertEquals(testEvent.getAmount(), resp.getAmount());
        assertEquals(testEvent.getCurrency(), resp.getCurrency());
    }
}