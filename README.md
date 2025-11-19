# Metrics & Profiles Service

The metrics-service ingests high-volume user interaction events, pushes them through Kafka, builds real-time preference profiles, and exposes them to downstream recommenders. It is built with Java 21, Spring Boot 3.2, Kafka 3.6, PostgreSQL 15, Redis 7, and Micrometer/Actuator for observability.

## Architecture

```
Frontend SDK ─┐
              │  (REST + JWT)
              ▼
        Event Ingestion API ──► Validation & Dedup (Redis)
                                   │
                                   ▼
                          Kafka (3 topics + DLT)
                                   │
                        Batched Kafka Consumers
                                   │
                                   ▼
                     Profile Builder + PostgreSQL (Flyway)
                                   │
             ┌─────────────────────┴──────────────────────┐
             ▼                                            ▼
    Profile Retrieval API                        Admin / Replay API
```

Key components live in `metrics-service/`:

- `controller/EventsController` – REST ingestion endpoint.
- `service/ingestion/*` – validation, deduplication, rate limiting.
- `kafka/consumer/ProfileEventsListener` – batched Kafka listener with manual commits.
- `service/profile/*` – profile builder plus user-facing services.
- `dto/response/UserProfileDto` – contract consumed by recommenders.

## Getting Started

### Prerequisites

- Java 21
- Docker / Docker Compose
- `make` or shell access

### Local Environment

```bash
# Start dependencies (Postgres, Redis, Kafka/ZooKeeper)
docker compose up -d postgres redis kafka zookeeper

# Run the metrics service
./gradlew :metrics-service:bootRun

# Tail logs
docker compose logs -f kafka redis
```

### Configuration

All relevant settings live in `metrics-service/src/main/resources/application.yml`. Highlights:

| Property | Purpose | Default |
|----------|---------|---------|
| `spring.kafka.consumer.group-id` | Dedicated profile consumer group | `metrics-profile-consumer` |
| `spring.kafka.consumer.properties.max.poll.records` | Batch size | `500` |
| `spring.kafka.producer.properties.compression.type` | Network compression | `zstd` |
| `app.kafka.topics.retention` | Topic retention window | `P7D` |
| `app.health.profile-processing.max-lag-ms` | SLA threshold for lag health | `1000` |

For a deep dive into event contracts, profile math, and integration points see:

- `docs/EVENT_SCHEMA.md`
- `docs/PROFILE_ALGORITHM.md`
- `docs/INTEGRATION.md`

## APIs

### Event Ingestion

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/events` | Accepts a batch of heterogeneous events (see EVENT_SCHEMA.md) |

### Profile Retrieval & Preferences

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/profiles/{userId}` | Retrieve full user profile |
| `GET` | `/api/v1/profiles/{userId}/preferences` | Snapshot of preferences (auto-creates profile) |
| `PUT` | `/api/v1/profiles/{userId}/preferences` | Manual override for category weights (owner/Admin) |
| `DELETE` | `/api/v1/profiles/{userId}` | Remove a profile (owner/Admin) |

### Admin Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/admin/profiles/recalculate` | Rebuild every profile from historical events |
| `GET` | `/api/v1/admin/profiles/export?format=csv\|json` | Export profiles for ML |
| `POST` | `/api/v1/admin/profiles/events/replay?userId=...` | Replay a single user’s events |

Swagger UI is available at `http://localhost:8082/swagger-ui.html`.

## Testing

Integration coverage relies on Testcontainers (Kafka, PostgreSQL, Redis). Two suites validate the full pipeline:

- `EventIngestionIntegrationTest` – validates request → Kafka publication, deduplication, and validation errors.
- `ProfileBuilderIntegrationTest` – verifies Kafka-driven profile updates, category weighting, and entity counting with mocked `ContentServiceClient`.

Run all tests:

```bash
./gradlew :metrics-service:test
```

## Performance & Reliability

- **Kafka Consumer Tuning**: batched listeners (`max.poll.records=500`), manual acknowledgements, configurable concurrency, and record-level parallelism per partition.
- **Producer Optimizations**: linger/batch size bumped plus `zstd` compression to reduce I/O.
- **Database Optimizations**: Hibernate batching (`batch_size=50`, ordered inserts/updates) and Flyway indexes (`V4__profile_indexes.sql`) keep profile writes under 5s P95.
- **Redis-Based Deduplication**: event and batch TTL = 24h.

## Observability

### Metrics

Metric | Description | Tags
------ | ----------- | ----
`events.ingested.total` | Accepted events by type (ingestion) | `type`
`events.processed.total` | Kafka events processed | `type`
`events.processing.lag` | Latest lag (Kafka → DB) gauge (ms) | —
`profiles.created.total` | Count of persisted profiles | —
`profiles.updated.total` | Count of profile updates | —
`category.preference.distribution` | Histogram of preference scores | `category`

Expose metrics via `/actuator/metrics`.

### Health Indicators

Health Check | Description
----------- | -----------
`kafka` | Checks cluster id & broker count via AdminClient
`contentService` | Pings content-service `/actuator/health`
`profileProcessing` | Reports DOWN when lag exceeds `app.health.profile-processing.max-lag-ms`

All available at `/actuator/health`.

## Integration Overview

- Frontend integration details: `docs/INTEGRATION.md`
- Event schema reference: `docs/EVENT_SCHEMA.md`
- Profile math (EMA weights, entity counters, behavioral signals): `docs/PROFILE_ALGORITHM.md`

## Useful Commands

```bash
# Format + build metrics-service only
./gradlew :metrics-service:clean :metrics-service:build

# Run metrics-service with dev profile
SPRING_PROFILES_ACTIVE=dev ./gradlew :metrics-service:bootRun

# Tail profile metrics
curl -s http://localhost:8082/actuator/metrics | jq
```

## License

MIT License.
