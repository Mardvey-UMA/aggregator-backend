# Profile Calculation Algorithms

The profile builder transforms raw events into real-time user preferences. This document explains the main signals and how they are updated.

## Activity Counters

| Field | Trigger | Notes |
|-------|---------|-------|
| `totalViews` | `post_view` | Increment per event |
| `totalClicks` | `post_click` | Increment per event |
| `totalLikes` / `totalDislikes` | `post_reaction` | Reaction specific |
| `totalBookmarks` | `post_reaction` (`bookmark`) | |
| `avgDwellTimeSeconds` | `post_dwell` | EMA with α = 0.2 |

## Category Preferences (EMA)

Each content metadata object delivers category → score pairs. Scores are combined with an exponential moving average to keep values within `[0,1]`.

```
updated = α * incomingScore + (1 - α) * current
```

α depends on signal strength:

| Event | α |
|-------|---|
| `post_reaction` (like) | 0.30 |
| `post_reaction` (dislike) | -0.15 (negative push) |
| `post_dwell` | 0.20 |
| `post_view` | 0.10 |

Negative α values push preferences down. All scores are clamped to `[0,1]` before persisting, and the histogram metric `category.preference.distribution{category=<name>}` records every updated score for monitoring.

## Entity Preferences

Metadata entities are provided as `Map<String, List<String>>` (e.g., `people`, `companies`). For every occurrence the counter increments:

```
entityPreferences[entityType][entityName] += 1
```

This builds light-weight frequency tables consumed later by the recommender.

## Behavioral Signals

- **Clickbait tolerance**: `+0.05` when clickbait flagged article is completed, `-0.02` when quality content is completed. Clamped to `[0,1]`.
- **Recent activity** (`viewedPostsLast7d`, `likedPostsLast30d`, `dislikedPostsLast30d`): bounded arrays (50 items) storing most recent identifiers.
- **Active hours**: array updated via session events (future work).

## Persistence & Upserts

Profiles are unique on `user_id`. Kafka batches are processed partition-by-partition and persisted via Hibernate batching (`batch_size=50`). Inserts and updates are ordered to minimize row locks, and Flyway migration `V4__profile_indexes.sql` keeps lookups efficient.

## Metrics

| Metric | When | Purpose |
|--------|------|---------|
| `profiles.created.total` | New profile instantiated | Capacity planning |
| `profiles.updated.total` | Profile saved | Throughput |
| `events.processed.total{type}` | Every Kafka event processed | Lag/volume |
| `events.processing.lag` | Difference between event timestamp and persistence time | SLA enforcement |

Monitor `profileProcessing` health indicator – it flips to `DOWN` if `events.processing.lag` exceeds `app.health.profile-processing.max-lag-ms`.

