# Content Service

Content aggregation and recommendation service for personalized content delivery.

## Overview

The Content Service is a microservice that handles:
- Content aggregation from multiple sources
- Personalized feed recommendations
- Content discovery and search
- Bookmark management
- View tracking and trending

## Features

- **Personalized Feeds**: Rule-based recommendation with diversity enforcement
- **Trending Content**: Popular content based on recent engagement
- **Bookmarks**: User bookmark management
- **Search**: Full-text content search
- **Category Browsing**: Content organized by categories
- **Similar Content**: Related content recommendations
- **Caching**: Redis-based caching for performance

## Tech Stack

- Java 21
- Spring Boot 3.2+
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL 15
- Redis 7
- Resilience4j Circuit Breaker
- Flyway Migrations
- OpenAPI/Swagger

## API Endpoints

### Public Endpoints
- `GET /api/v1/content/trending` - Get trending content

### Authenticated Endpoints
- `GET /api/v1/content/feed` - Get personalized feed
- `GET /api/v1/content/{id}` - Get content by ID
- `GET /api/v1/content/{id}/similar` - Get similar content
- `GET /api/v1/content/search?q=query` - Search content
- `GET /api/v1/content/category/{category}` - Get by category
- `POST /api/v1/content/{id}/bookmark` - Bookmark content
- `DELETE /api/v1/content/{id}/bookmark` - Remove bookmark
- `GET /api/v1/content/bookmarks` - Get user bookmarks

### Admin Endpoints (Requires ADMIN role)
- `POST /api/v1/admin/content` - Create content
- `PUT /api/v1/admin/content/{id}` - Update content
- `DELETE /api/v1/admin/content/{id}` - Delete content
- `DELETE /api/v1/admin/content/all` - Delete all content
- `POST /api/v1/admin/content/seed?count=100` - Seed test data
- `GET /api/v1/admin/content/stats` - Get statistics
- `POST /api/v1/admin/content/cache/clear` - Clear caches

## Setup Instructions

### Prerequisites

- Java 21 JDK
- Docker and Docker Compose
- PostgreSQL 15 (or use Docker)
- Redis 7 (or use Docker)

### Local Development

1. **Clone and navigate to backend:**
   ```bash
   cd aggregation-project/backend
   ```

2. **Start dependencies with Docker Compose:**
   ```bash
   docker-compose up -d content-postgres redis
   ```

3. **Build the service:**
   ```bash
   ./gradlew :content-service:build
   ```

4. **Run the service:**
   ```bash
   ./gradlew :content-service:bootRun
   ```

5. **Access the API:**
   - API: http://localhost:8081
   - Swagger UI: http://localhost:8081/swagger-ui.html
   - Health: http://localhost:8081/actuator/health

### Running with Docker

```bash
# Build image
docker build -t content-service content-service/

# Run with Docker Compose
docker-compose up content-service
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | 8081 |
| `CONTENT_DB_HOST` | PostgreSQL host | localhost |
| `CONTENT_DB_PORT` | PostgreSQL port | 5432 |
| `CONTENT_DB_NAME` | Database name | contentdb |
| `CONTENT_DB_USERNAME` | Database username | postgres |
| `CONTENT_DB_PASSWORD` | Database password | postgres |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `AUTH_SERVICE_URL` | Auth service URL | http://localhost:8080 |

### Application Properties

See `src/main/resources/application.yml` for full configuration.

Key settings:
- `app.cache.feed-ttl-minutes`: Feed cache TTL (default: 5)
- `app.cache.trending-ttl-minutes`: Trending cache TTL (default: 15)
- `app.content.feed-page-size`: Default page size (default: 20)

## Testing

### Run All Tests

```bash
./gradlew :content-service:test
```

### Run Integration Tests

Integration tests use Testcontainers for PostgreSQL and Redis:

```bash
./gradlew :content-service:test --tests "*IntegrationTest"
```

### Run Unit Tests

```bash
./gradlew :content-service:test --tests "*Test" --exclude "*IntegrationTest"
```

### Test Coverage

```bash
./gradlew :content-service:jacocoTestReport
```

Report available at: `build/reports/jacoco/test/html/index.html`

## Database Migrations

Flyway migrations are in `src/main/resources/db/migration/`.

Run migrations:
```bash
./gradlew :content-service:flywayMigrate
```

## Health Checks

Custom health indicators:
- `contentDatabase` - PostgreSQL connectivity
- `redis` - Redis connectivity
- `authService` - Auth service availability
- `contentAvailability` - Minimum content threshold

Access at: `GET /actuator/health`

## Metrics

Prometheus metrics available at: `GET /actuator/prometheus`

Custom metrics:
- `content.feed.requests` - Feed request counter
- `content.feed.latency` - Feed latency timer
- `content.cache.hit_ratio` - Cache hit ratio gauge
- `content.diversity.score` - Feed diversity gauge
- `content.recommendation.score` - Score distribution

## Caching Strategy

Caches with TTLs:
- `feed`: 5 minutes
- `coldStartFeed`: 10 minutes
- `trending`: 15 minutes
- `content`: 60 minutes
- `similarContent`: 30 minutes
- `userPreferences`: 30 minutes

## Logging

Structured JSON logging in production. Configure levels:

```yaml
logging:
  level:
    com.contentaggregation.content: DEBUG
    org.springframework.web: INFO
```

## Troubleshooting

### Common Issues

1. **Auth service connection refused**
   - Ensure auth-service is running
   - Check `AUTH_SERVICE_URL` configuration

2. **Redis connection failed**
   - Verify Redis is running: `redis-cli ping`
   - Check `REDIS_HOST` and `REDIS_PORT`

3. **Database migration failed**
   - Check PostgreSQL is running
   - Verify credentials in configuration

### Useful Commands

```bash
# Check service health
curl http://localhost:8081/actuator/health

# View metrics
curl http://localhost:8081/actuator/prometheus

# Seed test data
curl -X POST http://localhost:8081/api/v1/admin/content/seed?count=50

# Clear caches
curl -X POST http://localhost:8081/api/v1/admin/content/cache/clear
```

## License

Apache 2.0
