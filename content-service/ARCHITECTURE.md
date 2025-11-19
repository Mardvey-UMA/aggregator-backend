# Content Service Architecture

This document describes the architecture and design decisions of the Content Service.

## Service Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Content Service                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Content   │  │    Admin    │  │   Health    │              │
│  │ Controller  │  │ Controller  │  │  Endpoints  │              │
│  └──────┬──────┘  └──────┬──────┘  └─────────────┘              │
│         │                │                                      │
│  ┌──────┴────────────────┴──────┐                               │
│  │    Security Filter Chain     │                               │
│  │   (JWT Auth via AuthService) │                               │
│  └──────────────┬───────────────┘                               │
│                 │                                                │
│  ┌──────────────┴───────────────┐                               │
│  │   Recommendation Service     │                               │
│  │   (Rule-based Algorithm)     │                               │
│  └──────────────┬───────────────┘                               │
│                 │                                                │
│  ┌──────────────┴───────────────┐                               │
│  │        Cache Layer           │                               │
│  │      (Redis Caching)         │                               │
│  └──────────────┬───────────────┘                               │
│                 │                                                │
│  ┌──────────────┴───────────────┐                               │
│  │      Repository Layer        │                               │
│  │    (Spring Data JPA)         │                               │
│  └──────────────┬───────────────┘                               │
│                 │                                                │
└─────────────────┼───────────────────────────────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
┌───────┴───────┐   ┌───────┴───────┐   ┌───────────────┐
│  PostgreSQL   │   │     Redis     │   │ Auth Service  │
│  (Content DB) │   │   (Cache)     │   │  (JWT Valid)  │
└───────────────┘   └───────────────┘   └───────────────┘
```

## Data Model

### Entity Relationships

```
┌─────────────────┐       ┌──────────────────┐
│   ContentPost   │──────<│ ContentMetadata  │
├─────────────────┤  1:1  ├──────────────────┤
│ id              │       │ id               │
│ externalId      │       │ contentPostId    │
│ source          │       │ categories (JSON)│
│ sourceChannelId │       │ entities (JSON)  │
│ title           │       │ keywords (JSON)  │
│ content         │       │ sentiment        │
│ contentType     │       │ sentimentScore   │
│ contentLength   │       │ language         │
│ hasMedia        │       │ readingTime      │
│ mediaType       │       │ complexityScore  │
│ mediaUrls (JSON)│       └──────────────────┘
│ linkUrl         │
│ publishedAt     │
└────────┬────────┘
         │
         │ 1:N
         ▼
┌─────────────────┐       ┌─────────────────┐
│  ContentView    │       │ ContentBookmark │
├─────────────────┤       ├─────────────────┤
│ id              │       │ id              │
│ contentPostId   │       │ userId          │
│ userId          │       │ contentPostId   │
│ sessionId       │       │ createdAt       │
│ viewedAt        │       └─────────────────┘
└─────────────────┘
```

### Database Indexes

```sql
-- Content Posts
CREATE INDEX idx_content_posts_external_id ON content_posts(external_id);
CREATE INDEX idx_content_posts_source ON content_posts(source);
CREATE INDEX idx_content_posts_published_at ON content_posts(published_at);
CREATE INDEX idx_content_posts_content_type ON content_posts(content_type);
CREATE INDEX idx_content_posts_source_channel ON content_posts(source_channel_id);

-- Content Metadata (GIN for JSONB)
CREATE INDEX idx_metadata_categories ON content_metadata USING GIN (categories);
CREATE INDEX idx_metadata_keywords ON content_metadata USING GIN (keywords);

-- Views and Bookmarks
CREATE INDEX idx_views_content_post ON content_views(content_post_id);
CREATE INDEX idx_views_viewed_at ON content_views(viewed_at);
CREATE INDEX idx_bookmarks_user ON content_bookmarks(user_id);
CREATE UNIQUE INDEX idx_bookmarks_user_content ON content_bookmarks(user_id, content_post_id);
```

## Recommendation Algorithm

### Scoring Formula

```
Score = CategoryMatch × 10
      + ContentTypeMatch × 3
      + RecencyDecay
      + TrendingBonus
      + RandomFactor
```

Where:
- **CategoryMatch**: 10.0 if content category matches user preference
- **ContentTypeMatch**: 3.0 if content type matches preference
- **RecencyDecay**: 0-5 points based on publish date (newer = higher)
- **TrendingBonus**: 5.0 if content is trending (high views)
- **RandomFactor**: 0-2 random points for exploration

### Diversity Rules

1. **Category Diversity**: Maximum 3 consecutive items from same category
2. **Media Mix**: Insert media content every 4th position
3. **Cold Start**: Use popularity-weighted random selection

```java
// Diversity enforcement
private static final int MAX_CONSECUTIVE_SAME_CATEGORY = 3;
private static final int MEDIA_INSERT_INTERVAL = 4;

// Check consecutive same category
if (lastCategory.equals(currentCategory)) {
    consecutiveCount++;
    if (consecutiveCount >= MAX_CONSECUTIVE_SAME_CATEGORY) {
        // Skip this item or reorder
    }
}
```

### Cold Start Handling

For new users without preferences:
1. Mix content from all categories equally
2. Prioritize recent high-engagement content
3. Include variety of content types
4. Apply random factor for exploration

## Caching Strategy

### Cache Hierarchy

```
┌─────────────────────────────────────┐
│        Request Cache (Local)        │  ← Fastest (microseconds)
├─────────────────────────────────────┤
│         Redis Cache (Remote)        │  ← Fast (milliseconds)
├─────────────────────────────────────┤
│      Database (PostgreSQL)          │  ← Slower (tens of ms)
└─────────────────────────────────────┘
```

### Cache Names and TTLs

| Cache Name | TTL | Description |
|------------|-----|-------------|
| `feed` | 5 min | Personalized feed for user |
| `coldStartFeed` | 10 min | Generic feed for new users |
| `trending` | 15 min | Trending content list |
| `content` | 60 min | Individual content items |
| `similarContent` | 30 min | Similar content recommendations |
| `userPreferences` | 30 min | User preference cache |

### Cache Key Strategy

```java
// Feed cache key includes user preferences
String feedKey = "feed:" + userId + ":" + categories.hashCode();

// Content cache key
String contentKey = "content:" + contentId;

// Trending is global
String trendingKey = "trending:" + limit;
```

### Cache Eviction

```java
// Evict related caches when content changes
@CacheEvict(value = {"feed", "coldStartFeed", "trending"}, allEntries = true)
public ContentDto createContent(CreateContentRequest request) {
    // ...
}

// Evict specific content cache
@CacheEvict(value = "content", key = "#id")
public void deleteContent(UUID id) {
    // ...
}
```

## Security Architecture

### Authentication Flow

```
Client → Content Service → Auth Service
         (JWT Token)       (Validate)
              ↓                 ↓
         Extract Token    Return User
              ↓                 ↓
         UserPrincipal ←───────┘
              ↓
         Process Request
```

### JWT Validation via Auth Service

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(request, response, chain) {
        String token = extractToken(request);
        if (token != null) {
            try {
                // Call auth service to validate
                UserDto user = authServiceClient.validateToken(token);

                // Create security context
                UserPrincipal principal = UserPrincipal.from(user);
                SecurityContextHolder.getContext()
                    .setAuthentication(principal);
            } catch (Exception e) {
                // Invalid token
            }
        }
        chain.doFilter(request, response);
    }
}
```

### Circuit Breaker Protection

```java
@CircuitBreaker(name = "authService", fallbackMethod = "validateTokenFallback")
@Retry(name = "authService")
public UserDto validateToken(String token) {
    return webClient.get()
            .uri("/api/v1/auth/me")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(UserDto.class)
            .block();
}

// Fallback when auth service is down
public UserDto validateTokenFallback(String token, Exception ex) {
    throw new AuthenticationException("Auth service unavailable");
}
```

Circuit breaker settings:
- Failure threshold: 50%
- Wait duration in open state: 10 seconds
- Sliding window size: 10 calls

## Performance Optimizations

### Database Optimizations

1. **Connection Pooling (HikariCP)**
   ```yaml
   hikari:
     maximum-pool-size: 20
     minimum-idle: 5
     connection-timeout: 30000
   ```

2. **Batch Operations**
   ```java
   @BatchSize(size = 25)
   @OneToOne(fetch = FetchType.LAZY)
   private ContentMetadata metadata;
   ```

3. **Query Hints**
   ```java
   @QueryHints({
       @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
       @QueryHint(name = "org.hibernate.readOnly", value = "true")
   })
   Page<ContentPost> findByPublishedAtAfter(LocalDateTime after, Pageable pageable);
   ```

### Async Processing

```java
@Configuration
public class AsyncConfig {
    @Bean("virtualThreadExecutor")
    public AsyncTaskExecutor virtualThreadExecutor() {
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
```

## Monitoring

### Health Indicators

1. **ContentDatabaseHealthIndicator** - PostgreSQL connectivity
2. **RedisHealthIndicator** - Redis connectivity
3. **AuthServiceHealthIndicator** - Auth service availability
4. **ContentAvailabilityHealthIndicator** - Minimum content threshold

### Metrics

```
# Counters
content.feed.requests
content.bookmarks.total
content.views.total
content.search.requests

# Timers
content.feed.latency

# Gauges
content.cache.hit_ratio
content.diversity.score
content.total.count

# Distribution
content.recommendation.score
```

## Future ML Integration Plan

### Phase 1: Data Collection (Current)
- Track user interactions (views, bookmarks, time spent)
- Store user preference signals
- Build category affinity profiles

### Phase 2: Feature Engineering
- User features: category preferences, content type preferences, activity patterns
- Content features: categories, sentiment, complexity, engagement metrics
- Interaction features: view count, bookmark rate, CTR

### Phase 3: Model Training
- Collaborative filtering for similar users
- Content-based filtering using embeddings
- Hybrid approach combining both

### Phase 4: Real-time Inference
- Deploy model as separate service
- A/B testing framework
- Feedback loop for continuous improvement

### Architecture with ML

```
┌─────────────────┐     ┌─────────────────┐
│ Content Service │────>│   ML Service    │
│  (Candidates)   │     │  (Ranking)      │
└────────┬────────┘     └────────┬────────┘
         │                       │
         └───────────┬───────────┘
                     │
              ┌──────┴──────┐
              │ Feature Store│
              │  (Redis)     │
              └─────────────┘
```

## Deployment Architecture

### Docker Compose (Development)

```yaml
services:
  content-service:
    depends_on:
      - content-postgres
      - redis
      - auth-service
    environment:
      - CONTENT_DB_HOST=content-postgres
      - REDIS_HOST=redis
      - AUTH_SERVICE_URL=http://auth-service:8080
```

### Kubernetes (Production)

```yaml
# Deployment
replicas: 3
resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

# Horizontal Pod Autoscaler
minReplicas: 3
maxReplicas: 10
targetCPUUtilization: 70%
```

## Error Handling

### Error Categories

1. **Validation Errors (400)** - Invalid input
2. **Authentication Errors (401)** - Invalid/missing token
3. **Authorization Errors (403)** - Insufficient permissions
4. **Not Found Errors (404)** - Resource not found
5. **Conflict Errors (409)** - Duplicate operations
6. **Service Errors (503)** - Dependency unavailable

### Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/v1/content/feed",
  "errors": [
    {"field": "q", "message": "must be at least 2 characters"}
  ]
}
```

## Scaling Considerations

### Horizontal Scaling
- Stateless service design
- Session affinity not required
- Redis for distributed caching

### Vertical Scaling
- Virtual threads for I/O operations
- Connection pooling optimization
- JVM tuning for heap and GC

### Database Scaling
- Read replicas for heavy read traffic
- Materialized views for trending
- Table partitioning by date (future)
