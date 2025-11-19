# Integration Guide

This guide explains how the frontend SDK and the recommender service interact with the metrics-service.

## Frontend → Metrics-Service

### Authentication

- All ingestion requests require a user JWT (validated via `JwtAuthenticationFilter`).
- The `userId` inside each event **must** match the authenticated principal.

### Sending Events

1. Buffer events client-side (max 1000).
2. POST to `/api/v1/events` with body:

```json
{
  "batchId": "0ab7fdbf-79a2-4c2a-9958-3fa17a56f16b",
  "timestamp": "2024-11-19T18:25:43.511Z",
  "events": [
    { "base": { ... }, "data": { ... } }
  ]
}
```

3. Handle partial success:
   - `status="accepted"` – all events ingested.
   - `status="partial"` – inspect `errors[]` for per-event issues.
   - `status="duplicate"` – batch already processed; safe to drop locally.

### Deduplication Rules

- `batchId` → 24h TTL.
- `eventId` → 24h TTL.
- Returning the same batch is idempotent at least once.

## Metrics-Service → Kafka

- Topics: `user-events-engagement`, `user-events-reactions`, `user-events-impressions`, `user-profile-updates` (future).
- Partition key: `userId` (preserves per-user ordering).
- Producer compression: `zstd`.

## Kafka → Profile Builder

- Batched consumers pull up to 500 records per poll.
- Manual ack after partition processing ensures "process then commit".
- Average profile update latency must remain `<5s`; health indicator enforces.

## Recommender → Metrics-Service

### Fetching Profiles

- Endpoint: `GET /api/v1/profiles/{userId}`
- Response: `UserProfileDto` (see code for fields).
- 5‑minute Redis cache ensures quick read paths.

### Preferences-only Snapshot

- Endpoint: `GET /api/v1/profiles/{userId}/preferences`
- Returns `{ categories, contentTypes, entities }`.
- Auto-creates default profile for cold-start users.

### Manual Overrides

Admins (or the user) can override category weights:

```http
PUT /api/v1/profiles/{userId}/preferences
Authorization: Bearer <token>
{
  "categories": {
    "technology": 0.8,
    "finance": 0.4
  }
}
```

## Operational Workflows

| Scenario | Endpoint | Notes |
|----------|----------|-------|
| Resync entire dataset | `POST /api/v1/admin/profiles/recalculate` | Replays all stored events |
| Export for ML training | `GET /api/v1/admin/profiles/export?format=csv` | Streams file attachment |
| Replay single user | `POST /api/v1/admin/profiles/events/replay?userId=...&fromDate=2024-11-01T00:00:00Z` | Deletes + rebuilds |

## Monitoring & Alerts

- Scrape `/actuator/metrics` (Micrometer + Prometheus format).
- Alert on:
  - `events.processing.lag` > 1000 ms (profileProcessing health toggles).
  - `profiles.updated.total` drop to 0 (stalled consumer).
  - `kafka` health indicator down.

## Error Handling Expectations

- 4xx for validation errors (client action needed).
- 429 when rate limit exceeded (retry with exponential backoff).
- 5xx only on unexpected server conditions (automated retry recommended).

