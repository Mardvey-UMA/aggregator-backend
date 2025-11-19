package com.contentaggregation.metrics.controller;

import com.contentaggregation.metrics.client.AuthServiceClient;
import com.contentaggregation.metrics.config.KafkaConfig;
import com.contentaggregation.metrics.dto.event.EventBatchRequest;
import com.contentaggregation.metrics.dto.response.EventIngestionResponse;
import com.contentaggregation.metrics.security.UserPrincipal;
import com.contentaggregation.metrics.service.ingestion.EventIngestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private static final Logger log = LoggerFactory.getLogger(EventsController.class);

    private final EventIngestionService ingestionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final AuthServiceClient authServiceClient;

    public EventsController(
        EventIngestionService ingestionService,
        KafkaTemplate<String, Object> kafkaTemplate,
        RedisTemplate<String, String> redisTemplate,
        JdbcTemplate jdbcTemplate,
        AuthServiceClient authServiceClient
    ) {
        this.ingestionService = ingestionService;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.authServiceClient = authServiceClient;
    }

    @PostMapping
    public ResponseEntity<EventIngestionResponse> ingestEvents(
        @Valid @RequestBody EventBatchRequest request,
        @RequestHeader(value = "X-Request-ID", required = false) String requestIdHeader
    ) {
        UserPrincipal principal = currentUser();
        UUID userId = principal.getId();
        UUID requestId = resolveRequestId(requestIdHeader);

        EventIngestionResponse response = ingestionService.ingestEventBatch(request, userId, requestId);

        log.debug("Ingestion result for batch {}: {}", request.batchId(), response.status());

        if (response.isDuplicate()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("kafka", checkKafka());
        status.put("redis", checkRedis());
        status.put("database", checkDatabase());
        status.put("authService", authServiceClient.isHealthy());
        return status;
    }

    private boolean checkKafka() {
        try {
            kafkaTemplate.partitionsFor(KafkaConfig.USER_EVENTS_IMPRESSIONS);
            return true;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            var factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                return false;
            }
            try (var connection = factory.getConnection()) {
                return connection.ping() != null;
            }
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (DataAccessException e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }

    private UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException("Authenticated user not found in security context");
    }

    private UUID resolveRequestId(String headerValue) {
        try {
            return headerValue != null ? UUID.fromString(headerValue) : UUID.randomUUID();
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }
}

