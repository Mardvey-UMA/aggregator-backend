# Phase 2: Content Service with Stub Recommendations

## Overview

Implement a content aggregation service that serves diverse content types through a stub recommendation system. While the actual ML-based recommender will be implemented later, this phase creates the complete infrastructure and API contracts that make the system appear production-ready from the frontend perspective.

**Duration**: 2-3 days
**Complexity**: Medium
**Priority**: High (core user-facing feature)

---

## Objectives

### Primary Goals
1. ✅ Create content data model supporting multiple content types
2. ✅ Implement stub recommendation service with hardcoded diverse content
3. ✅ Build content retrieval API with pagination and filtering
4. ✅ Integrate with auth service for user context
5. ✅ Implement Redis caching for performance
6. ✅ Add content metadata enrichment (categories, entities, sentiment)
7. ✅ Create flexible content types system
8. ✅ Prepare for future ML recommender integration

### Secondary Goals
1. ✅ Add content search functionality
2. ✅ Implement content bookmarking
3. ✅ Add trending content endpoints
4. ✅ Create admin API for content management
5. ✅ Add comprehensive API documentation

---

## Technical Stack

### Core Technologies
- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.1+
- **Spring Data JPA**: Repository layer
- **PostgreSQL**: 15+ (content database)
- **Redis**: 7+ (caching, rankings)
- **Flyway**: Database migrations
- **Gradle**: Build tool (Kotlin DSL)

### Integration
- **Spring WebClient**: Communication with auth service
- **Circuit Breaker**: Resilience4j for fault tolerance
- **Service Discovery**: Docker/K8s networking (no Eureka)

### Testing
- **JUnit 5**: Unit testing
- **Mockito**: Mocking
- **Testcontainers**: Integration tests
- **WireMock**: Mock auth service

---

## Architecture Overview

### Service Structure
```
content-service/
├── src/main/java/com/contentaggregation/content/
│   ├── config/
│   ├── controller/
│   ├── service/
│   │   ├── recommendation/    # Stub recommender
│   │   ├── content/           # Content management
│   │   └── enrichment/        # Metadata enrichment
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── client/                # Auth service client
│   └── ContentServiceApplication.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/
```

### Database Schema
```sql
-- Content posts table
content_posts
├── id (UUID, PK)
├── external_id (VARCHAR, UNIQUE) -- from source
├── source (VARCHAR) -- telegram, vk, rss, etc.
├── source_channel_id (VARCHAR)
├── source_channel_name (VARCHAR)
├── title (TEXT, nullable)
├── content (TEXT) -- main content
├── content_type (VARCHAR) -- article, short_post, video, image
├── content_length (INTEGER) -- character count
├── has_media (BOOLEAN)
├── media_type (VARCHAR) -- video, image, gallery, null
├── media_urls (JSONB) -- array of media URLs
├── link_url (VARCHAR, nullable)
├── published_at (TIMESTAMP)
├── created_at (TIMESTAMP)
└── updated_at (TIMESTAMP)

-- Content metadata (preprocessed features)
content_metadata
├── id (UUID, PK)
├── content_id (UUID, FK → content_posts)
├── categories (JSONB) -- {"tech": 0.9, "business": 0.3}
├── entities (JSONB) -- {"person": ["Elon Musk"], "company": ["Tesla"]}
├── keywords (TEXT[]) -- array of keywords
├── sentiment (VARCHAR) -- positive, negative, neutral
├── sentiment_score (FLOAT) -- -1.0 to 1.0
├── language (VARCHAR) -- ru, en, etc.
├── reading_time_minutes (INTEGER)
├── complexity_score (FLOAT) -- 0-1, readability
├── created_at (TIMESTAMP)
└── updated_at (TIMESTAMP)

-- User bookmarks
content_bookmarks
├── id (UUID, PK)
├── user_id (UUID) -- from auth service
├── content_id (UUID, FK → content_posts)
├── created_at (TIMESTAMP)
└── UNIQUE(user_id, content_id)

-- Content views (for trending)
content_views
├── id (UUID, PK)
├── content_id (UUID, FK → content_posts)
├── user_id (UUID, nullable)
├── viewed_at (TIMESTAMP)
└── session_id (UUID)
```

### API Endpoints

#### Public Endpoints (Require Authentication)
```
GET    /api/v1/content/feed              - Get personalized feed
GET    /api/v1/content/trending           - Get trending content
GET    /api/v1/content/{id}               - Get specific content
GET    /api/v1/content/search             - Search content
POST   /api/v1/content/{id}/bookmark      - Bookmark content
DELETE /api/v1/content/{id}/bookmark      - Remove bookmark
GET    /api/v1/content/bookmarks          - Get user bookmarks
```

#### Admin Endpoints (Require ADMIN role)
```
POST   /api/v1/admin/content              - Create content
PUT    /api/v1/admin/content/{id}         - Update content
DELETE /api/v1/admin/content/{id}         - Delete content
POST   /api/v1/admin/content/seed         - Seed database with test data
```

---

## Content Types & Diversity

### Content Type Enum
```java
public enum ContentType {
    SHORT_POST,      // < 500 characters, Twitter-like
    MEDIUM_POST,     // 500-2000 characters
    LONG_ARTICLE,    // > 2000 characters, up to 4096
    VIDEO_POST,      // Post with video
    IMAGE_POST,      // Post with images
    MIXED_MEDIA      // Post with multiple media types
}
```

### Content Categories
```
- Technology (AI, Programming, Gadgets)
- Science (Physics, Biology, Space)
- Business (Startups, Finance, Marketing)
- Entertainment (Movies, Music, Games)
- Sports (Football, Basketball, Esports)
- Lifestyle (Travel, Food, Fashion)
- Education (Courses, Tutorials, Books)
- News (World, Politics, Local)
```

### Stub Content Requirements

The stub recommender must generate diverse content:
- **Length**: Mix of short (300 chars), medium (1000 chars), long (4000 chars)
- **Media**: 40% text-only, 30% with images, 20% with video, 10% mixed
- **Categories**: Balanced distribution across all categories
- **Freshness**: Mix of recent (today), yesterday, this week
- **Sentiment**: 60% positive, 30% neutral, 10% negative

---

## Stub Recommendation Strategy

### Simple Rule-Based Algorithm

The stub recommender should simulate realistic behavior:

1. **First-time users (cold start)**:
    - Mix of popular content from all categories
    - Trending content from last 24 hours
    - Diverse content types to explore interests

2. **Users with onboarding preferences**:
    - 70% content from selected categories
    - 20% trending content
    - 10% exploration (other categories)

3. **Pagination**:
    - Page size: 20 items
    - Cursor-based pagination for infinite scroll
    - Deterministic ordering for caching

4. **Diversity rules**:
    - No more than 3 consecutive posts from same category
    - Mix content lengths (alternate short/long)
    - Insert media content every 4-5 posts

### Stub Implementation Pattern

```java
@Service
public class StubRecommendationService implements RecommendationService {
    
    public Page<ContentDto> getPersonalizedFeed(
        UUID userId, 
        Pageable pageable
    ) {
        // 1. Get user preferences (if any)
        UserPreferences prefs = getUserPreferences(userId);
        
        // 2. Build content query based on preferences
        List<ContentPost> candidates = getCandidateContent(prefs);
        
        // 3. Apply diversity rules
        List<ContentPost> diversified = applyDiversityRules(candidates);
        
        // 4. Score and rank (simple rule-based)
        List<ContentPost> ranked = applySimpleRanking(diversified);
        
        // 5. Return paginated
        return createPage(ranked, pageable);
    }
    
    private List<ContentPost> applyDiversityRules(List<ContentPost> content) {
        List<ContentPost> result = new ArrayList<>();
        String lastCategory = null;
        int categoryContinuity = 0;
        
        for (ContentPost post : content) {
            // Ensure diversity
            if (post.getCategory().equals(lastCategory)) {
                categoryContinuity++;
                if (categoryContinuity >= 3) {
                    continue; // Skip to ensure diversity
                }
            } else {
                categoryContinuity = 1;
                lastCategory = post.getCategory();
            }
            result.add(post);
        }
        
        return result;
    }
}
```

---

## Integration with Auth Service

### JWT Token Validation

```java
@Service
public class AuthServiceClient {
    
    private final WebClient webClient;
    
    public UserDto validateToken(String token) {
        return webClient.get()
            .uri("http://auth-service:8080/api/v1/auth/me")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(UserDto.class)
            .block();
    }
    
    public UserPreferences getUserPreferences(UUID userId) {
        // Fetch from auth service or cache
    }
}
```

### Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      authService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 10
        permitted-number-of-calls-in-half-open-state: 3
```

---

## Caching Strategy

### Redis Cache Layers

1. **Content Cache**:
    - Key: `content:{id}`
    - TTL: 1 hour
    - Invalidate on update

2. **Feed Cache**:
    - Key: `feed:{userId}:{page}`
    - TTL: 5 minutes
    - Invalidate on new content or user interaction

3. **Trending Cache**:
    - Key: `trending:{timeframe}`
    - TTL: 15 minutes
    - Recalculate periodically

4. **User Preferences Cache**:
    - Key: `user:preferences:{userId}`
    - TTL: 30 minutes
    - Invalidate on profile update

---

## Future ML Integration Points

### Design for Easy Swap

The stub service must be designed so the real recommender can replace it with minimal changes:

```java
// Interface that both stub and ML service implement
public interface RecommendationService {
    Page<ContentDto> getPersonalizedFeed(UUID userId, Pageable pageable);
    List<ContentDto> getSimilarContent(UUID contentId, int limit);
    List<ContentDto> getTrendingContent(int limit);
}

// Current stub implementation
@Service
@Profile("stub")
public class StubRecommendationService implements RecommendationService {
    // Simple rule-based logic
}

// Future ML implementation (not in Phase 2)
@Service
@Profile("ml")
public class MLRecommendationService implements RecommendationService {
    // ML model inference
}
```

### API Contract Compatibility

Ensure response DTOs include fields for ML features:
```java
public record ContentDto(
    UUID id,
    String title,
    String content,
    ContentType contentType,
    Map<String, Double> categories,
    
    // ML-ready fields (populated by stub for now)
    Double recommendationScore,  // 0-1, higher = better match
    String recommendationReason,  // "Popular in Technology"
    UUID recommendationId         // Track recommendation performance
) {}
```

---

## Success Criteria

### Functional Requirements
- ✅ User receives personalized content feed
- ✅ Content is diverse (types, categories, lengths)
- ✅ Pagination works smoothly (cursor-based)
- ✅ Bookmarking content works
- ✅ Trending content refreshes regularly
- ✅ Search returns relevant results
- ✅ Admin can seed test content
- ✅ Response times < 200ms with cache

### Non-Functional Requirements
- ✅ Handles 1000+ content items efficiently
- ✅ Cache hit rate > 80% for feeds
- ✅ Graceful degradation if auth service down
- ✅ All endpoints documented (OpenAPI)
- ✅ Integration tests cover all flows
- ✅ Database indexes optimize queries

### Frontend Integration Ready
- ✅ API contracts documented
- ✅ Response format consistent
- ✅ Error handling comprehensive
- ✅ CORS configured
- ✅ Examples provided

---

## Implementation Steps (Prompts)

Phase 2 consists of 4 sequential prompts:

### Prompt 1: Project Setup & Data Model (60 min)
- Add content-service module to project
- Create entities and repositories
- Write Flyway migrations
- Configure database connections

### Prompt 2: Stub Recommendation Service (90 min)
- Implement content seeding utility
- Create stub recommendation algorithm
- Implement diversity rules
- Add caching layer

### Prompt 3: REST API & Integration (75 min)
- Create all REST controllers
- Implement auth service client
- Add circuit breakers
- Configure OpenAPI docs

### Prompt 4: Testing & Optimization (60 min)
- Write integration tests
- Add performance tests
- Optimize database queries
- Finalize documentation

---

## Configuration Requirements

### Environment Variables

```bash
# Database
CONTENT_DB_HOST=localhost
CONTENT_DB_PORT=5432
CONTENT_DB_NAME=contentdb
CONTENT_DB_USERNAME=postgres
CONTENT_DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Auth Service Integration
AUTH_SERVICE_URL=http://localhost:8080
AUTH_SERVICE_TIMEOUT_MS=5000

# Content Configuration
CONTENT_FEED_PAGE_SIZE=20
CONTENT_CACHE_TTL_MINUTES=5
TRENDING_CACHE_TTL_MINUTES=15

# Server
SERVER_PORT=8081
```

---

## Monitoring & Observability

### Custom Metrics
- `content.feed.requests.total` - Feed endpoint hits
- `content.feed.cache.hit_ratio` - Cache effectiveness
- `content.recommendation.latency` - Recommendation time
- `content.diversity.score` - Diversity metric

### Health Checks
- Database connectivity
- Redis connectivity
- Auth service availability

---

## Phase Completion Checklist

Before moving to Phase 3:

### Code Complete
- [ ] All entities and repositories created
- [ ] Stub recommendation service implemented
- [ ] All REST controllers implemented
- [ ] Auth service client configured
- [ ] Caching layer working

### Tests Passing
- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Load tests acceptable
- [ ] Can seed 1000+ content items

### Documentation
- [ ] OpenAPI/Swagger complete
- [ ] API examples provided
- [ ] Frontend integration guide written
- [ ] README updated

### Deployment
- [ ] Docker image builds
- [ ] Runs in docker-compose
- [ ] Health checks passing
- [ ] Can fetch personalized feed
- [ ] Can bookmark content

---

## Next Phase Preview

**Phase 3: Metrics Collection Service** will implement:
- Event ingestion API
- Kafka integration for event streaming
- User profile updates based on events
- Real-time metrics processing
- Integration with content service for context

Phase 3 will consume events from frontend and update user profiles for future ML recommendations.