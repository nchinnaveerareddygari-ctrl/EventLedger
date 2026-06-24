package com.eventledger.service;

import com.eventledger.domain.Event;
import com.eventledger.domain.EventStatus;
import com.eventledger.domain.TransactionType;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Process an incoming event
     * Handles idempotency, validation, and calls Account Service
     */
    public EventResponse processEvent(EventRequest request) {
        String traceId = generateTraceId();
        log.info("Processing event: {} [traceId: {}]", request.getEventId(), traceId);

        // Check for duplicate
        Optional<Event> existingEvent = eventRepository.findByEventId(request.getEventId());
        if (existingEvent.isPresent()) {
            log.warn("Duplicate event detected: {} [traceId: {}]", request.getEventId(), traceId);
            Event event = existingEvent.get();
            event.setStatus(EventStatus.DUPLICATE);
            eventRepository.save(event);
            return mapEventToResponse(event);
        }

        // Validate transaction type
        TransactionType type = validateTransactionType(request.getType());

        // Create event entity
        Event event = new Event();
        event.setEventId(request.getEventId());
        event.setAccountId(request.getAccountId());
        event.setType(type);
        event.setAmount(request.getAmount());
        event.setCurrency(request.getCurrency());
        event.setEventTimestamp(request.getEventTimestamp());
        event.setStatus(EventStatus.PENDING);
        event.setTraceId(traceId);

        // Store metadata as JSON
        if (request.getMetadata() != null) {
            try {
                event.setMetadata(mapper.writeValueAsString(request.getMetadata()));
            } catch (Exception e) {
                log.error("Failed to serialize metadata [traceId: {}]", traceId, e);
            }
        }

        // Save event (idempotent storage)
        Event savedEvent = eventRepository.save(event);
        log.info("Event saved to gateway: {} [traceId: {}]", savedEvent.getEventId(), traceId);

        // Call Account Service to apply transaction
        try {
            accountServiceClient.applyTransaction(request, traceId);
            savedEvent.setStatus(EventStatus.PROCESSED);
            log.info("Event processed successfully: {} [traceId: {}]", savedEvent.getEventId(), traceId);
        } catch (Exception e) {
            savedEvent.setStatus(EventStatus.FAILED);
            log.error("Failed to process event with Account Service: {} [traceId: {}]", savedEvent.getEventId(), traceId, e);
            // Rethrow to be handled by controller
            throw new RuntimeException("Account Service unavailable: " + e.getMessage(), e);
        }

        eventRepository.save(savedEvent);
        return mapEventToResponse(savedEvent);
    }

    /**
     * Retrieve a single event by ID
     */
    public Optional<EventResponse> getEventById(Long id) {
        log.info("Retrieving event by ID: {}", id);
        return eventRepository.findById(id)
                .map(this::mapEventToResponse);
    }

    /**
     * Retrieve all events for an account (ordered by event timestamp)
     */
    public List<EventResponse> getEventsByAccount(String accountId) {
        log.info("Retrieving events for account: {}", accountId);
        return eventRepository.findByAccountIdOrderByTimestamp(accountId)
                .stream()
                .map(this::mapEventToResponse)
                .toList();
    }

    /**
     * Validate transaction type
     */
    private TransactionType validateTransactionType(String type) {
        try {
            return TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid transaction type: {}", type);
            throw new IllegalArgumentException("Invalid transaction type: " + type);
        }
    }

    /**
     * Generate a unique trace ID
     */
    private String generateTraceId() {
        return "trace-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Map Event entity to EventResponse DTO
     */
    private EventResponse mapEventToResponse(Event event) {
        Map<String, Object> metadata = null;
        if (event.getMetadata() != null) {
            try {
                metadata = mapper.readValue(event.getMetadata(), Map.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize metadata for event: {}", event.getEventId(), e);
            }
        }

        return EventResponse.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .accountId(event.getAccountId())
                .type(event.getType().toString())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .eventTimestamp(event.getEventTimestamp())
                .metadata(metadata)
                .status(event.getStatus().toString())
                .createdAt(event.getCreatedAt())
                .traceId(event.getTraceId())
                .build();
    }
}
