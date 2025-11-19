# Phase 3: Metrics Collection & User Profile Service

## Overview

Implement an event-driven metrics collection system that captures user interactions from the frontend, processes them through Kafka, and updates user profiles in real-time. This service is critical for building accurate user preference models for future ML-based recommendations.

**Duration**: 3-4 days
**Complexity**: High
**Priority**: Critical (foundation for personalization)

---

## Objectives

### Primary Goals
1. ✅ Create event ingestion API for frontend metrics
2. ✅ Integrate Apache Kafka for event streaming
3. ✅ Implement user profile builder (stream processing)
4. ✅ Build user profile storage and retrieval API
5. ✅ Process events to update user preferences in real-time
6. ✅ Integrate with content service for post metadata
7. ✅ Implement event deduplication and validation
8. ✅ Add monitoring and observability for event pipeline

### Secondary Goals
1. ✅ Add batch processing for historical events
2. ✅ Implement user profile aggregation queries
3. ✅ Create admin API for profile management
4. ✅ Add event replay capability
5. ✅ Export profiles for ML training

---

## Technical Stack

### Core Technologies
- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.1+
- **Apache Kafka**: 3.6+ (event streaming)
- **Spring Kafka**: Kafka integration
- **PostgreSQL**: 15+ (user profiles storage)
- **Redis**: 7+ (deduplication, caching)
- **Flyway**: Database migrations

### Stream Processing
- **Kafka Streams**: Real-time event processing
- **Spring Cloud Stream**: Stream binders

### Integration
- **Spring WebClient**: Communication with content-service
- **Circuit Breaker**: Resilience4j

---

## Architecture Overview

### Service Structure
```
metrics-service/
├── src/main/java/com/contentaggregation/metrics/
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   ├── KafkaStreamsConfig.java
│   │   └── WebClientConfig.java
│   ├── controller/
│   │   ├── EventsController.java
│   │   └── ProfileController.java
│   ├── service/
│   │   ├── ingestion/
│   │   │   ├── EventIngestionService.java
│   │   │   └── EventValidationService.java
│   │   ├── processing/
│   │   │   ├── EventProcessor.java
│   │   │   └── ProfileBuilderService.java
│   │   └── profile/
│   │       ├── UserProfileService.java
│   │       └── ProfileAggregationService.java
│   ├── repository/
│   │   ├── UserProfileRepository.java
│   │   ├── UserEventRepository.java
│   │   └── EventBatchRepository.java
│   ├── entity/
│   │   ├── UserProfile.java
│   │   ├── UserEvent.java
│   │   └── EventBatch.java
│   ├── dto/
│   │   ├── event/
│   │   │   ├── BaseEvent.java
│   │   │   ├── PostImpressionEvent.java
│   │   │   ├── PostViewEvent.java
│   │   │   ├── PostClickEvent.java
│   │   │   ├── PostDwellEvent.java
│   │   │   ├── PostReactionEvent.java
│   │   │   └── SessionEvent.java
│   │   ├── request/
│   │   │   └── EventBatchRequest.java
│   │   └── response/
│   │       └── UserProfileDto.java
│   ├── client/
│   │   └── ContentServiceClient.java
│   ├── kafka/
│   │   ├── producer/
│   │   │   └── EventProducer.java
│   │   └── streams/
│   │       ├── ProfileUpdateProcessor.java
│   │       └── AggregationProcessor.java
│   └── MetricsServiceApplication.java
```

### Event Flow Architecture

```
Frontend App
     ↓
  (HTTP POST)
     ↓
Events API (/api/v1/events)
     ↓
  Validation
     ↓
  Deduplication (Redis)
     ↓
 Kafka Producer → Topic: user-events-{type}
     ↓
 Kafka Streams Processing
     ↓
  Profile Updates
     ↓
PostgreSQL (user_profiles)
     ↓
Profile API (/api/v1/profiles/{userId})
     ↓
Recommender Service (future)
```

---

## Event Schema (from your description)

### Base Event Structure
```java
public record BaseEvent(
    String eventId,           // UUID
    String userId,            // UUID from auth service
    String sessionId,         // Session UUID
    Long timestamp,           // Unix timestamp (ms)
    String platform,          // "ios" | "android" | "web"
    String appVersion,        // e.g., "1.0.0"
    String eventType          // Event type identifier
) {}
```

### Event Types

1. **post_impression** - Post appeared in viewport
2. **post_view** - Post visible for >1.5 seconds
3. **post_click** - User opened post details
4. **post_dwell** - User finished reading post
5. **post_like** - User liked post
6. **post_dislike** - User disliked post
7. **post_bookmark** - User bookmarked post
8. **post_share** - User shared post
9. **session_start** - User session started
10. **session_end** - User session ended
11. **feed_scroll** - User scrolled feed

### Specific Event DTOs

**PostImpressionEvent**:
```java
public record PostImpressionEvent(
    BaseEvent base,
    ImpressionData data
) {}

public record ImpressionData(
    String messageId,              // Content ID
    Integer positionInFeed,        // 0-based position
    String recommendationId,       // Optional: tracking ID
    Double recommendationScore,    // Optional: score
    String source                  // "recommended" | "trending" | "search"
) {}
```

**PostViewEvent**:
```java
public record PostViewData(
    String messageId,
    Integer positionInFeed,
    Integer visibleDurationMs,     // How long visible
    Double viewportPercentage      // 0-1, how much was visible
) {}
```

**PostDwellEvent**:
```java
public record PostDwellData(
    String messageId,
    Integer dwellTimeMs,           // Total reading time
    Double scrollDepth,            // 0-1, how much scrolled
    Integer interactions           // Number of interactions
) {}
```

**PostReactionEvent** (like/dislike):
```java
public record PostReactionData(
    String messageId,
    Integer positionInFeed,
    Integer dwellTimeBeforeMs      // Time before reaction
) {}
```

---

## Database Schema

### user_profiles Table
```sql
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    
    -- Activity metrics
    total_views INTEGER DEFAULT 0,
    total_clicks INTEGER DEFAULT 0,
    total_likes INTEGER DEFAULT 0,
    total_dislikes INTEGER DEFAULT 0,
    total_bookmarks INTEGER DEFAULT 0,
    total_sessions INTEGER DEFAULT 0,
    
    -- Preferences (JSONB)
    category_preferences JSONB DEFAULT '{}',
    entity_preferences JSONB DEFAULT '{}',
    content_type_preferences JSONB DEFAULT '{}',
    style_preferences JSONB DEFAULT '{}',
    
    -- Engagement patterns
    avg_dwell_time_seconds FLOAT DEFAULT 0,
    avg_session_length_seconds INTEGER DEFAULT 0,
    active_hours INTEGER[] DEFAULT ARRAY[]::INTEGER[],
    preferred_content_length VARCHAR(20),  -- short, medium, long
    
    -- Behavioral signals
    clickbait_tolerance FLOAT DEFAULT 0.5,
    exploration_vs_exploitation FLOAT DEFAULT 0.5,
    
    -- Recent activity
    liked_posts_last_30d TEXT[] DEFAULT ARRAY[]::TEXT[],
    disliked_posts_last_30d TEXT[] DEFAULT ARRAY[]::TEXT[],
    viewed_posts_last_7d TEXT[] DEFAULT ARRAY[]::TEXT[],
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### user_events Table (TimescaleDB hypertable)
```sql
CREATE TABLE user_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    session_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    platform VARCHAR(20),
    app_version VARCHAR(20),
    request_id UUID,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Convert to hypertable (TimescaleDB)
SELECT create_hypertable('user_events', 'timestamp');
```

### event_batches Table
```sql
CREATE TABLE event_batches (
    id UUID PRIMARY KEY,
    batch_id UUID UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    event_count INTEGER NOT NULL,
    received_at TIMESTAMP NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Kafka Topics Structure

### Topic Naming
- `user-events-impressions` - Post impressions
- `user-events-engagement` - Views, clicks, dwells
- `user-events-reactions` - Likes, dislikes, bookmarks
- `user-events-sessions` - Session start/end
- `user-profile-updates` - Profile update commands

### Topic Configuration
```yaml
partitions: 8
replication-factor: 3 (production)
retention.ms: 604800000  # 7 days
compression.type: gzip
```

### Partitioning Strategy
- Partition by `userId` (hash)
- All events from same user → same partition
- Maintains event ordering per user

---

## Profile Update Algorithm

### Event Processing Logic

**For each event:**
1. Validate event structure
2. Check deduplication (Redis)
3. Publish to appropriate Kafka topic
4. Kafka Streams processes event
5. Fetch content metadata from content-service
6. Update user profile based on event type
7. Save updated profile to PostgreSQL

### Category Preference Update (EMA)

```java
// Exponential Moving Average for category scores
public void updateCategoryPreference(
    String category, 
    double newScore, 
    double alpha
) {
    double currentScore = categoryPreferences.getOrDefault(category, 0.0);
    double updatedScore = alpha * newScore + (1 - alpha) * currentScore;
    categoryPreferences.put(category, updatedScore);
}
```

**Alpha values by event type:**
- Like: α = 0.3 (strong signal)
- Bookmark: α = 0.4 (very strong signal)
- Dwell (complete read): α = 0.2
- View: α = 0.1 (weak signal)
- Dislike: α = -0.15 (negative signal)

### Entity Preference Update

```java
// Simple count-based for entities (people, companies, locations)
public void updateEntityPreference(String entityType, String entity) {
    Map<String, Integer> entities = entityPreferences
        .getOrDefault(entityType, new HashMap<>());
    
    int currentCount = entities.getOrDefault(entity, 0);
    entities.put(entity, currentCount + 1);
    entityPreferences.put(entityType, entities);
}
```

### Clickbait Tolerance Update

```java
// Based on whether user clicks and reads clickbait
public void updateClickbaitTolerance(boolean isClickbait, boolean completed) {
    if (isClickbait && completed) {
        // User reads clickbait → increase tolerance
        clickbaitTolerance = Math.min(1.0, clickbaitTolerance + 0.05);
    } else if (!isClickbait && completed) {
        // User prefers quality → decrease tolerance
        clickbaitTolerance = Math.max(0.0, clickbaitTolerance - 0.02);
    }
}
```

---

## API Endpoints

### Event Ingestion
```
POST /api/v1/events              - Receive event batch from frontend
GET  /api/v1/events/health       - Event pipeline health
```

### User Profiles
```
GET  /api/v1/profiles/{userId}              - Get user profile
GET  /api/v1/profiles/{userId}/preferences  - Get preferences only
PUT  /api/v1/profiles/{userId}/preferences  - Update preferences manually
DELETE /api/v1/profiles/{userId}            - Delete user profile
```

### Admin Endpoints
```
POST /api/v1/admin/events/replay            - Replay events for user
GET  /api/v1/admin/profiles/export          - Export profiles for ML
POST /api/v1/admin/profiles/recalculate     - Recalculate all profiles
```

---

## Integration with Content Service

### Content Metadata Enrichment

When processing events, fetch content metadata:

```java
@Service
public class ContentServiceClient {
    
    @CircuitBreaker(name = "contentService")
    public ContentMetadata getContentMetadata(String messageId) {
        return webClient.get()
            .uri("/api/v1/content/{id}/metadata", messageId)
            .retrieve()
            .bodyToMono(ContentMetadata.class)
            .block();
    }
}
```

**Retrieved data:**
- Categories with scores
- Entities (person, company, location)
- Content type
- Sentiment
- Is clickbait flag

---

## Deduplication Strategy

### Redis-based Deduplication

```java
@Service
public class EventDeduplicationService {
    
    private final RedisTemplate<String, String> redis;
    private static final int DEDUP_TTL_SECONDS = 86400; // 24 hours
    
    public boolean isDuplicate(String eventId) {
        String key = "event:dedup:" + eventId;
        Boolean isNew = redis.opsForValue()
            .setIfAbsent(key, "1", DEDUP_TTL_SECONDS, TimeUnit.SECONDS);
        return !Boolean.TRUE.equals(isNew);
    }
    
    public boolean isBatchDuplicate(String batchId) {
        String key = "batch:dedup:" + batchId;
        Boolean isNew = redis.opsForValue()
            .setIfAbsent(key, "1", DEDUP_TTL_SECONDS, TimeUnit.SECONDS);
        return !Boolean.TRUE.equals(isNew);
    }
}
```

---

## Kafka Streams Processing

### Profile Update Topology

```java
@Configuration
public class ProfileUpdateTopology {
    
    @Bean
    public KStream<String, UserEvent> processUserEvents(
        StreamsBuilder builder
    ) {
        // Read from all event topics
        KStream<String, UserEvent> events = builder
            .stream(List.of(
                "user-events-impressions",
                "user-events-engagement",
                "user-events-reactions"
            ));
        
        // Group by userId
        KGroupedStream<String, UserEvent> grouped = events
            .groupByKey();
        
        // Aggregate into profile updates
        KTable<String, ProfileUpdate> profileUpdates = grouped
            .aggregate(
                ProfileUpdate::new,
                (userId, event, aggregate) -> {
                    return updateProfile(aggregate, event);
                },
                Materialized.as("profile-aggregates")
            );
        
        // Sink to database
        profileUpdates.toStream()
            .foreach((userId, update) -> {
                saveProfileToDatabase(userId, update);
            });
        
        return events;
    }
}
```

---

## Success Criteria

### Functional Requirements
- ✅ Frontend can send event batches
- ✅ Events deduplicated correctly
- ✅ Events published to Kafka
- ✅ Kafka Streams processes events
- ✅ User profiles updated in real-time
- ✅ Profile API returns current state
- ✅ Category preferences calculated accurately
- ✅ Integration with content service works

### Non-Functional Requirements
- ✅ Event ingestion latency < 100ms (P95)
- ✅ Profile update latency < 5 seconds
- ✅ Handles 10,000 events/sec
- ✅ Zero event loss (Kafka durability)
- ✅ Deduplication accuracy > 99.9%
- ✅ All endpoints documented

### Data Quality
- ✅ No duplicate events processed
- ✅ Category scores in valid range [0, 1]
- ✅ Entity counts accurate
- ✅ Timestamps validated
- ✅ User profiles consistent

---

## Implementation Steps (Prompts)

Phase 3 consists of 5 sequential prompts:

### Prompt 1: Project Setup & Kafka Configuration (60 min)
- Add metrics-service module
- Configure Kafka and Kafka Streams
- Create event DTOs
- Setup database schema

### Prompt 2: Event Ingestion API (75 min)
- Implement event validation
- Add deduplication service
- Create event producer
- Build REST API for events

### Prompt 3: Stream Processing & Profile Builder (90 min)
- Implement Kafka Streams topology
- Build profile update algorithms
- Integrate with content service
- Create profile repository

### Prompt 4: Profile API & Admin Tools (60 min)
- Create profile retrieval API
- Add profile management endpoints
- Implement admin tools
- Add monitoring metrics

### Prompt 5: Testing & Optimization (75 min)
- Write integration tests
- Add performance tests
- Optimize stream processing
- Complete documentation

---

## Monitoring & Observability

### Custom Metrics
- `events.ingested.total` - Counter
- `events.processing.latency` - Timer
- `events.duplicates.total` - Counter
- `profiles.updates.total` - Counter
- `kafka.lag` - Gauge (consumer lag)

### Health Indicators
- Kafka connectivity
- Database connectivity
- Content service availability
- Event processing lag

---

## Phase Completion Checklist

Before moving to Phase 4:

### Code Complete
- [ ] All event DTOs created
- [ ] Event ingestion API working
- [ ] Kafka Streams processing events
- [ ] Profile updates happening
- [ ] Profile API functional

### Tests Passing
- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Event deduplication works
- [ ] Profile calculations correct

### Deployment
- [ ] Kafka cluster running
- [ ] Metrics service deployed
- [ ] Can send events from frontend
- [ ] Profiles updated in real-time

---

## Next Phase Preview

**Phase 4: User Onboarding Service** will implement:
- Onboarding flow for new users
- Category and content type selection
- Initial preference seeding
- Integration with profile service
- Frontend-ready onboarding API

Phase 4 will use the profile system to store user preferences from onboarding.