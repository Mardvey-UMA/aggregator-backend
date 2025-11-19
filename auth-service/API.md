# Auth Service API Documentation

## Base URL

- Development: `http://localhost:8080/api/v1`
- Production: `https://api.contentaggregation.com/api/v1`

## Authentication

Most endpoints require JWT Bearer token authentication.

```
Authorization: Bearer <access_token>
```

## Endpoints

### Authentication

#### Register User

Creates a new user account.

```
POST /auth/register
```

**Request Body:**

```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePass123"
}
```

**Validation Rules:**
- `email`: Valid email format, unique
- `username`: 3-50 characters, unique
- `password`: Min 8 characters, must contain uppercase, lowercase, and digit

**Response:** `201 Created`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "username": "johndoe",
    "emailVerified": false,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**Errors:**
- `409 Conflict`: Email or username already exists

---

#### Login

Authenticates user with credentials.

```
POST /auth/login
```

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "username": "johndoe",
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**Errors:**
- `401 Unauthorized`: Invalid credentials

---

#### Refresh Token

Obtains new access token using refresh token.

```
POST /auth/refresh
```

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "username": "johndoe",
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**Errors:**
- `401 Unauthorized`: Invalid or expired refresh token

---

#### Logout

Revokes current refresh token.

```
POST /auth/logout
```

**Headers:** `Authorization: Bearer <access_token>`

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:** `200 OK`

```json
{
  "message": "Logged out successfully"
}
```

---

### OAuth2 - VK

#### Get VK Authorization URL

Returns URL to redirect user to VK for authentication.

```
GET /auth/oauth2/vk/url
```

**Response:** `200 OK`

```json
{
  "url": "https://oauth.vk.com/authorize?client_id=...&redirect_uri=...&response_type=code&scope=email"
}
```

---

#### VK OAuth2 Callback

Handles OAuth2 callback from VK.

```
GET /auth/oauth2/callback/vk?code={authorization_code}
```

**Query Parameters:**
- `code`: Authorization code from VK

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "username": "vk_12345678",
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**Errors:**
- `401 Unauthorized`: Failed to authenticate with VK

---

### User Management

#### Get Current User

Returns the authenticated user's profile.

```
GET /users/me
```

**Headers:** `Authorization: Bearer <access_token>`

**Response:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "johndoe",
  "emailVerified": true,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

#### Update Current User

Updates the authenticated user's profile.

```
PUT /users/me
```

**Headers:** `Authorization: Bearer <access_token>`

**Request Body:**

```json
{
  "username": "newusername",
  "currentPassword": "CurrentPass123",
  "newPassword": "NewSecurePass456"
}
```

**Notes:**
- `username`: Optional, 3-50 characters
- `currentPassword` and `newPassword`: Both required when changing password

**Response:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "newusername",
  "emailVerified": true,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Errors:**
- `400 Bad Request`: Validation failed or incorrect current password
- `409 Conflict`: Username already taken

---

#### Delete Current User

Deletes the authenticated user's account.

```
DELETE /users/me
```

**Headers:** `Authorization: Bearer <access_token>`

**Response:** `204 No Content`

---

## Error Responses

All errors follow RFC 7807 Problem Details format.

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    },
    {
      "field": "password",
      "message": "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    }
  ]
}
```

### Common Error Codes

| Status | Error | Description |
|--------|-------|-------------|
| 400 | Bad Request | Invalid request body or parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource already exists |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

---

## Rate Limiting

API requests are rate limited per IP address.

- **Limit**: 100 requests per minute
- **Headers**:
  - `X-RateLimit-Remaining`: Requests remaining
  - `X-RateLimit-Replenish-Rate`: Tokens added per second

When limit exceeded:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "path": "/api/v1/auth/login"
}
```

---

## JWT Token Structure

### Access Token

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "user@example.com",
  "iat": 1705316400,
  "exp": 1705317300,
  "type": "access",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "roles": "ROLE_USER"
}
```

**Expiration:** 15 minutes (configurable)

### Refresh Token

**Payload:**
```json
{
  "sub": "user@example.com",
  "iat": 1705316400,
  "exp": 1705921200,
  "type": "refresh",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "tokenId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Expiration:** 7 days (configurable)

---

## OpenAPI Specification

Interactive API documentation available at:

- **Swagger UI**: `/swagger-ui.html`
- **OpenAPI JSON**: `/api-docs`
- **OpenAPI YAML**: `/api-docs.yaml`

---

## SDK Examples

### cURL

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","username":"johndoe","password":"SecurePass123"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"SecurePass123"}'

# Get current user
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."

# Refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"eyJhbGciOiJIUzI1NiIs..."}'
```

### JavaScript/TypeScript

```typescript
const API_URL = 'http://localhost:8080/api/v1';

// Register
const register = async (email: string, username: string, password: string) => {
  const response = await fetch(`${API_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, username, password })
  });
  return response.json();
};

// Login
const login = async (email: string, password: string) => {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  return response.json();
};

// Get current user
const getCurrentUser = async (accessToken: string) => {
  const response = await fetch(`${API_URL}/users/me`, {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
  return response.json();
};

// Refresh token
const refreshToken = async (refreshToken: string) => {
  const response = await fetch(`${API_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  return response.json();
};
```

### Python

```python
import requests

API_URL = 'http://localhost:8080/api/v1'

def register(email: str, username: str, password: str):
    response = requests.post(
        f'{API_URL}/auth/register',
        json={'email': email, 'username': username, 'password': password}
    )
    return response.json()

def login(email: str, password: str):
    response = requests.post(
        f'{API_URL}/auth/login',
        json={'email': email, 'password': password}
    )
    return response.json()

def get_current_user(access_token: str):
    response = requests.get(
        f'{API_URL}/users/me',
        headers={'Authorization': f'Bearer {access_token}'}
    )
    return response.json()

def refresh_token(refresh_token: str):
    response = requests.post(
        f'{API_URL}/auth/refresh',
        json={'refreshToken': refresh_token}
    )
    return response.json()
```
