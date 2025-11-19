# Event Schema

This document describes every event type emitted by the frontend SDK and accepted by the metrics-service ingestion API.

## Envelope

Every event shares the same base structure:

```json
{
  "base": {
    "eventId": "e8d5b4b8-4c78-4b1f-91c4-04199dca5db8",
    "userId": "96798538-3d69-4618-8da3-020bec1233fe",
    "sessionId": "35f17392-cd26-4a81-bc25-a700c3f7d40e",
    "timestamp": "2024-11-19T17:10:45.123Z",
    "platform": "web",
    "appVersion": "1.12.3",
    "eventType": "post_view"
  },
  "data": { "...event specific payload..." }
}
```

Rules:

- `eventId`, `sessionId`, `userId` – UUID v4, deduplicated in Redis.
- `timestamp` – ISO instant; rejected if older than `app.events.validation.max-age` or `max-future-skew`.
- `platform` – `web`, `ios`, or `android`.
- `eventType` – see table below.

## Event Types

| Type | Payload | Description |
|------|---------|-------------|
| `post_impression` | `messageId`, `positionInFeed`, optional `recommendationId`, `recommendationScore`, `source` | Card entered viewport |
| `post_view` | `messageId`, `positionInFeed`, `visibleDurationMs`, `viewportPercentage` | Card stayed visible for ≥1.5s |
| `post_click` | `messageId`, `positionInFeed`, `dwellBeforeMs` | User opened details |
| `post_dwell` | `messageId`, `dwellTimeMs`, `scrollDepth`, `interactions` | Completion level + scroll |
| `post_reaction` | `messageId`, `reactionType` (`like`,`dislike`,`bookmark`,`share`), `positionInFeed`, `dwellTimeBeforeMs` | Engagement signal |
| `session` | `sessionId`, `type` (`start`/`end`), `durationMs`, `device` | Session boundaries |

See `metrics-service/src/main/java/com/contentaggregation/metrics/dto/event/*` for typed DTOs.

## Batch Ingestion

- Endpoint: `POST /api/v1/events`
- Request body: `EventBatchRequest { batchId, timestamp, events[] }`
- Constraints:
  - Max events per batch: 1000 (enforced by rate limiter).
  - `batchId` deduplicated for 24 hours.

## Validation

1. Envelope fields present and match authenticated user.
2. Timestamp within `[now - maxAge, now + maxFutureSkew]`.
3. Per-event semantic validation (viewport %, dwell > 0, etc.).
4. Deduplicated `batchId` + `eventId` via Redis.

Invalid records are rejected with `ValidationError { eventId, field, message }` while valid records in the same batch continue through the pipeline.

