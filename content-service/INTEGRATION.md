# Content Service Integration Guide

This guide explains how to integrate with the Content Service API.

## Authentication Flow

### 1. Obtain JWT Token from Auth Service

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### 2. Use Token in Content Service Requests

Include the token in the `Authorization` header:

```bash
curl http://localhost:8081/api/v1/content/feed \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## API Usage Examples

### Fetch Personalized Feed

```bash
# Basic feed request
curl http://localhost:8081/api/v1/content/feed \
  -H "Authorization: Bearer $TOKEN"

# With pagination
curl "http://localhost:8081/api/v1/content/feed?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# With category preferences
curl "http://localhost:8081/api/v1/content/feed?categories=technology,science&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Latest AI Developments",
      "content": "Article content...",
      "contentType": "ARTICLE",
      "source": "RSS",
      "sourceChannelName": "Tech News",
      "linkUrl": "https://example.com/article",
      "mediaUrls": ["https://example.com/image.jpg"],
      "mediaType": "IMAGE",
      "publishedAt": "2024-01-15T10:30:00",
      "primaryCategory": "technology",
      "keywords": ["AI", "machine learning"],
      "recommendationScore": 0.85,
      "viewCount": 1250,
      "bookmarkCount": 45,
      "isBookmarked": false
    }
  ],
  "metadata": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### JavaScript/TypeScript Example

```typescript
const API_URL = 'http://localhost:8081/api/v1';

interface FeedResponse {
  content: ContentDto[];
  metadata: PageMetadata;
}

async function getFeed(token: string, page = 0, size = 20): Promise<FeedResponse> {
  const response = await fetch(
    `${API_URL}/content/feed?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok) {
    throw new Error(`Feed request failed: ${response.status}`);
  }

  return response.json();
}

// Usage
const feed = await getFeed(accessToken);
console.log(`Loaded ${feed.content.length} items`);
```

### Search Content

```bash
# Basic search
curl "http://localhost:8081/api/v1/content/search?q=artificial%20intelligence" \
  -H "Authorization: Bearer $TOKEN"

# With pagination
curl "http://localhost:8081/api/v1/content/search?q=machine%20learning&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Bookmark Content

```bash
# Create bookmark
curl -X POST http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000/bookmark \
  -H "Authorization: Bearer $TOKEN"

# Response
{"message": "Bookmark created"}

# Remove bookmark
curl -X DELETE http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000/bookmark \
  -H "Authorization: Bearer $TOKEN"

# Get user's bookmarks
curl http://localhost:8081/api/v1/content/bookmarks \
  -H "Authorization: Bearer $TOKEN"
```

### View Specific Content

```bash
# Get content by ID (also records a view)
curl http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Session-Id: unique-session-id"
```

The `X-Session-Id` header prevents duplicate view counts from the same session.

### Get Trending Content

```bash
# No authentication required
curl "http://localhost:8081/api/v1/content/trending?limit=10"
```

### Get Similar Content

```bash
curl "http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000/similar?limit=5" \
  -H "Authorization: Bearer $TOKEN"
```

### Browse by Category

```bash
curl "http://localhost:8081/api/v1/content/category/technology?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## Error Handling

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created (bookmark created) |
| 204 | No Content (bookmark deleted) |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (invalid/missing token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found (content not found) |
| 409 | Conflict (already bookmarked) |
| 503 | Service Unavailable (auth service down) |

### Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/v1/content/feed",
  "errors": [
    {
      "field": "q",
      "message": "Search query must be at least 2 characters"
    }
  ]
}
```

### Handling Token Expiration

```typescript
async function fetchWithRefresh(url: string, options: RequestInit) {
  let response = await fetch(url, options);

  if (response.status === 401) {
    // Token expired, refresh it
    const newToken = await refreshToken();
    options.headers = {
      ...options.headers,
      'Authorization': `Bearer ${newToken}`
    };
    response = await fetch(url, options);
  }

  return response;
}
```

## Rate Limiting

Current limits:
- 100 requests per minute per user
- 1000 requests per minute per IP (anonymous)

Headers returned:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705315200
```

When rate limited:
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 30 seconds."
}
```

## Pagination

All list endpoints support pagination:

| Parameter | Description | Default |
|-----------|-------------|---------|
| `page` | Page number (0-indexed) | 0 |
| `size` | Items per page | 20 |
| `sort` | Sort field | publishedAt |
| `direction` | Sort direction (ASC/DESC) | DESC |

Example:
```bash
curl "http://localhost:8081/api/v1/content/feed?page=2&size=10&sort=publishedAt&direction=DESC" \
  -H "Authorization: Bearer $TOKEN"
```

## Best Practices

### 1. Use Session IDs for View Tracking

Always include `X-Session-Id` header when fetching content details to prevent duplicate view counting:

```typescript
const sessionId = localStorage.getItem('sessionId') || generateSessionId();

fetch(`${API_URL}/content/${contentId}`, {
  headers: {
    'Authorization': `Bearer ${token}`,
    'X-Session-Id': sessionId
  }
});
```

### 2. Implement Infinite Scroll

```typescript
async function loadMoreContent(page: number) {
  const response = await getFeed(token, page, 20);

  if (response.metadata.hasNext) {
    // Show "Load More" button or trigger on scroll
  }

  return response.content;
}
```

### 3. Cache Responses Client-Side

```typescript
const cache = new Map<string, { data: any; timestamp: number }>();
const CACHE_TTL = 60000; // 1 minute

async function getCachedFeed(page: number) {
  const key = `feed-${page}`;
  const cached = cache.get(key);

  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.data;
  }

  const data = await getFeed(token, page);
  cache.set(key, { data, timestamp: Date.now() });
  return data;
}
```

### 4. Handle Network Errors

```typescript
async function safeFetch(url: string, options: RequestInit) {
  try {
    const response = await fetch(url, options);

    if (!response.ok) {
      const error = await response.json();
      throw new ApiError(error.message, response.status);
    }

    return response.json();
  } catch (error) {
    if (error instanceof TypeError) {
      // Network error
      throw new NetworkError('Unable to connect to server');
    }
    throw error;
  }
}
```

## WebSocket Events (Future)

Real-time updates will be available via WebSocket:

```typescript
const ws = new WebSocket('ws://localhost:8081/ws/content');

ws.onmessage = (event) => {
  const update = JSON.parse(event.data);

  switch (update.type) {
    case 'NEW_CONTENT':
      // Add new content to feed
      break;
    case 'TRENDING_UPDATE':
      // Update trending list
      break;
  }
};
```

## Testing Your Integration

### 1. Use the Seed Endpoint

```bash
# Seed 100 test items
curl -X POST "http://localhost:8081/api/v1/admin/content/seed?count=100" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 2. Verify Health

```bash
curl http://localhost:8081/actuator/health
```

### 3. Check Metrics

```bash
curl http://localhost:8081/actuator/prometheus | grep content
```

## Support

For issues or questions:
- GitHub Issues: https://github.com/contentaggregation/content-service/issues
- Email: support@contentaggregation.com
