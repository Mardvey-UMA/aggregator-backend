# Content Service API Examples

Collection of curl commands for common API workflows.

## Prerequisites

Set your auth token:
```bash
export TOKEN="your-jwt-token-here"
```

## Authentication

### Get JWT Token from Auth Service

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!"
  }'
```

Expected response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Register New User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "username": "newuser",
    "password": "SecurePass123!"
  }'
```

---

## Content Feed

### Get Personalized Feed

```bash
curl http://localhost:8081/api/v1/content/feed \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Breaking: New AI Development",
      "content": "Scientists announce breakthrough in artificial intelligence...",
      "contentType": "ARTICLE",
      "source": "RSS",
      "sourceChannelName": "Tech Daily",
      "linkUrl": "https://example.com/ai-news",
      "mediaUrls": ["https://example.com/images/ai.jpg"],
      "mediaType": "IMAGE",
      "publishedAt": "2024-01-15T10:30:00",
      "primaryCategory": "technology",
      "keywords": ["AI", "technology", "research"],
      "recommendationScore": 0.92,
      "viewCount": 1523,
      "bookmarkCount": 89,
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

### Get Feed with Pagination

```bash
curl "http://localhost:8081/api/v1/content/feed?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Feed with Category Preferences

```bash
curl "http://localhost:8081/api/v1/content/feed?categories=technology,science&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Feed with Content Type Preferences

```bash
curl "http://localhost:8081/api/v1/content/feed?contentTypes=ARTICLE,VIDEO&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Trending Content

### Get Trending Content (No Auth Required)

```bash
curl "http://localhost:8081/api/v1/content/trending?limit=10"
```

Expected response:
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "title": "Viral Video: Amazing Discovery",
    "contentType": "VIDEO",
    "viewCount": 50000,
    "bookmarkCount": 2500
  }
]
```

---

## Content Details

### Get Specific Content

```bash
curl http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Session-Id: $(uuidgen)"
```

Expected response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Breaking: New AI Development",
  "content": "Full article content here...",
  "contentType": "ARTICLE",
  "source": "RSS",
  "publishedAt": "2024-01-15T10:30:00",
  "viewCount": 1524,
  "bookmarkCount": 89,
  "isBookmarked": true
}
```

### Get Similar Content

```bash
curl "http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000/similar?limit=5" \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "title": "Related: AI Ethics Discussion",
    "primaryCategory": "technology"
  }
]
```

---

## Search

### Search Content

```bash
curl "http://localhost:8081/api/v1/content/search?q=artificial%20intelligence" \
  -H "Authorization: Bearer $TOKEN"
```

### Search with Pagination

```bash
curl "http://localhost:8081/api/v1/content/search?q=machine%20learning&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "content": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440003",
      "title": "Machine Learning Fundamentals",
      "content": "Introduction to ML concepts..."
    }
  ],
  "metadata": {
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

---

## Category Browsing

### Get Content by Category

```bash
curl "http://localhost:8081/api/v1/content/category/technology?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Bookmarks

### Create Bookmark

```bash
curl -X POST http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000/bookmark \
  -H "Authorization: Bearer $TOKEN"
```

Expected response (201 Created):
```json
{
  "message": "Bookmark created"
}
```

### Remove Bookmark

```bash
curl -X DELETE http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000/bookmark \
  -H "Authorization: Bearer $TOKEN"
```

Expected response: 204 No Content

### Get All Bookmarks

```bash
curl http://localhost:8081/api/v1/content/bookmarks \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Bookmarked Article",
      "isBookmarked": true
    }
  ],
  "metadata": {
    "page": 0,
    "size": 20,
    "totalElements": 5
  }
}
```

---

## Admin Operations

### Seed Test Data

```bash
curl -X POST "http://localhost:8081/api/v1/admin/content/seed?count=100" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected response:
```json
{
  "message": "Seeded 100 content items",
  "count": 100,
  "durationMs": 2345
}
```

### Create Content Manually

```bash
curl -X POST http://localhost:8081/api/v1/admin/content \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Manual Content Entry",
    "content": "This is manually created content for testing purposes.",
    "contentType": "ARTICLE",
    "categories": {"technology": 0.9, "education": 0.5},
    "keywords": ["test", "manual", "content"],
    "sourceChannelName": "Admin"
  }'
```

Expected response (201 Created):
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440004",
  "title": "Manual Content Entry",
  "contentType": "ARTICLE"
}
```

### Update Content

```bash
curl -X PUT http://localhost:8081/api/v1/admin/content/990e8400-e29b-41d4-a716-446655440004 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "content": "Updated content text."
  }'
```

### Delete Content

```bash
curl -X DELETE http://localhost:8081/api/v1/admin/content/990e8400-e29b-41d4-a716-446655440004 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected response: 204 No Content

### Get Statistics

```bash
curl http://localhost:8081/api/v1/admin/content/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected response:
```json
{
  "totalContent": 150,
  "timestamp": 1705315200000
}
```

### Clear All Caches

```bash
curl -X POST http://localhost:8081/api/v1/admin/content/cache/clear \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected response:
```json
{
  "message": "All caches cleared"
}
```

---

## Health & Monitoring

### Check Health

```bash
curl http://localhost:8081/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "contentDatabase": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "responseTimeMs": 5
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "redis": "Connected",
        "responseTimeMs": 2
      }
    },
    "authService": {
      "status": "UP"
    },
    "contentAvailability": {
      "status": "UP",
      "details": {
        "contentCount": 150,
        "minimumThreshold": 10
      }
    }
  }
}
```

### Get Metrics

```bash
curl http://localhost:8081/actuator/prometheus | grep content
```

Expected output:
```
content_feed_requests_total 1234.0
content_feed_latency_seconds{quantile="0.95"} 0.15
content_cache_hit_ratio 0.85
content_total_count 150.0
```

---

## Error Handling Examples

### Unauthorized Request

```bash
curl http://localhost:8081/api/v1/content/feed
```

Response (401):
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required",
  "path": "/api/v1/content/feed"
}
```

### Invalid Token

```bash
curl http://localhost:8081/api/v1/content/feed \
  -H "Authorization: Bearer invalid-token"
```

Response (401):
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### Content Not Found

```bash
curl http://localhost:8081/api/v1/content/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer $TOKEN"
```

Response (404):
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Content not found with id: 00000000-0000-0000-0000-000000000000"
}
```

### Already Bookmarked

```bash
# Second attempt to bookmark same content
curl -X POST http://localhost:8081/api/v1/content/550e8400-e29b-41d4-a716-446655440000/bookmark \
  -H "Authorization: Bearer $TOKEN"
```

Response (409):
```json
{
  "error": "Content already bookmarked"
}
```

### Invalid Search Query

```bash
curl "http://localhost:8081/api/v1/content/search?q=a" \
  -H "Authorization: Bearer $TOKEN"
```

Response (400):
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Search query must be at least 2 characters"
}
```

---

## Complete Workflow Example

```bash
#!/bin/bash

# 1. Login
echo "Logging in..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}' \
  | jq -r '.accessToken')

# 2. Get personalized feed
echo "Getting feed..."
curl -s http://localhost:8081/api/v1/content/feed \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.content[0].id' -r > /tmp/content_id

CONTENT_ID=$(cat /tmp/content_id)

# 3. View specific content
echo "Viewing content..."
curl -s http://localhost:8081/api/v1/content/$CONTENT_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Session-Id: $(uuidgen)" \
  | jq '.title'

# 4. Bookmark content
echo "Bookmarking..."
curl -s -X POST http://localhost:8081/api/v1/content/$CONTENT_ID/bookmark \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.'

# 5. Get similar content
echo "Getting similar content..."
curl -s "http://localhost:8081/api/v1/content/$CONTENT_ID/similar?limit=3" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.[].title'

# 6. Search
echo "Searching..."
curl -s "http://localhost:8081/api/v1/content/search?q=technology" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.content | length'

# 7. Get bookmarks
echo "Getting bookmarks..."
curl -s http://localhost:8081/api/v1/content/bookmarks \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.content | length'

echo "Done!"
```
