# Phase 1: Authentication & Authorization Service

## Overview

Migrate existing Kotlin-based authentication system to Java 21 with Spring Boot 3.2+. Implement VK OAuth2, email/password authentication, JWT token management, and prepare infrastructure for microservices architecture.

**Duration**: 3-4 days
**Complexity**: High
**Priority**: Critical (foundation for all other services)

---

## Objectives

### Primary Goals

1. ✅ Migrate Kotlin authentication service to Java 21
2. ✅ Implement VK OAuth2 authentication flow
3. ✅ Implement email/password registration and login
4. ✅ Configure Spring Security with JWT tokens
5. ✅ Enable virtual threads for I/O operations
6. ✅ Setup Docker containerization
7. ✅ Configure PostgreSQL database with migrations
8. ✅ Implement Redis caching for tokens and sessions

### Secondary Goals

1. ✅ Add comprehensive API documentation (OpenAPI/Swagger)
2. ✅ Implement rate limiting for authentication endpoints
3. ✅ Add health checks and monitoring (Actuator)
4. ✅ Write integration tests with Testcontainers
5. ✅ Prepare Kubernetes deployment manifests

---

## Technical Stack

### Core Technologies

- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.1+
- **Spring Security**: 6.x
- **Spring OAuth2 Client**: For VK integration
- **Spring Data JPA**: Repository layer
- **PostgreSQL**: 15+ (primary database)
- **Redis**: 7+ (caching, rate limiting)
- **Flyway**: Database migrations
- **Gradle**: Build tool (Kotlin DSL)

### Authentication & Security

- **JWT Library**: `io.jsonwebtoken:jjwt` (0.12.3)
- **BCrypt**: Password hashing (built into Spring Security)
- **OAuth2**: VK provider integration

### Testing

- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework
- **Testcontainers**: Integration testing with real databases
- **Spring Security Test**: Security testing utilities
- **RestAssured**: API testing

### DevOps & Deployment

- **Docker**: Containerization
- **Docker Compose**: Local development environment
- **Kubernetes**: Production deployment (optional in Phase 1)

---

## Architecture Overview

### Microservices Structure

```
content-aggregation-backend/
├── common/                    # Shared DTOs, exceptions, utilities
├── auth-service/             # Authentication & authorization
├── docker-compose.yml        # Local development stack
├── settings.gradle.kts       # Multi-module configuration
└── build.gradle.kts          # Root build configuration
```

### Database Schema

```
users
├── id (UUID, PK)
├── email (VARCHAR, UNIQUE)
├── username (VARCHAR, UNIQUE)
├── password_hash (VARCHAR, nullable for OAuth)
├── email_verified (BOOLEAN)
├── enabled (BOOLEAN)
├── roles (VARCHAR) - comma-separated
├── created_at (TIMESTAMP)
├── updated_at (TIMESTAMP)
└── last_login_at (TIMESTAMP)

oauth_accounts
├── id (UUID, PK)
├── user_id (UUID, FK → users)
├── provider (VARCHAR) - VK, GOOGLE, etc.
├── provider_id (VARCHAR) - external user ID
├── provider_username (VARCHAR)
├── provider_email (VARCHAR)
├── access_token (VARCHAR) - encrypted
├── refresh_token (VARCHAR) - encrypted
├── expires_at (TIMESTAMP)
├── created_at (TIMESTAMP)
└── updated_at (TIMESTAMP)
UNIQUE(provider, provider_id)

refresh_tokens
├── id (UUID, PK)
├── user_id (UUID, FK → users)
├── token (VARCHAR, UNIQUE)
├── expires_at (TIMESTAMP)
├── revoked (BOOLEAN)
├── device_info (VARCHAR)
├── ip_address (VARCHAR)
├── created_at (TIMESTAMP)
└── used_at (TIMESTAMP)
```

### API Endpoints

#### Public Endpoints (No Authentication)

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
GET    /api/v1/auth/oauth2/vk/authorize
GET    /api/v1/auth/oauth2/vk/callback
GET    /actuator/health
```

#### Protected Endpoints (Requires JWT)

```
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
PUT    /api/v1/auth/me
DELETE /api/v1/auth/me
```

---

## Reference Project Integration

### Important Context

Your existing Kotlin authentication project is located at:

```
.claude/context/examples/kotlinDating/auth-service
.claude/context/examples/kotlinDating/backend
```

**CRITICAL**: Before implementing any code in the prompts, you MUST:

1. **Study the Kotlin project structure**:

   - Read all source files
   - Understand package organization
   - Note configuration patterns
   - Review entity relationships

2. **Analyze existing implementations**:

   - How is VK OAuth2 configured?
   - What JWT library is used?
   - How are tokens generated and validated?
   - What security filters exist?
   - How is password encoding handled?

3. **Maintain compatibility**:

   - Same database schema (if sharing database)
   - Same API contracts (if frontend depends on them)
   - Same authentication flows
   - Similar error response formats

4. **Migrate patterns to Java 21**:
   - Convert Kotlin data classes → Java records
   - Convert Kotlin coroutines → Virtual threads
   - Use modern Java idioms (pattern matching, sealed types)
   - Maintain business logic

---

## Success Criteria

### Functional Requirements

- ✅ User can register with email/password
- ✅ User can login with email/password
- ✅ User can authenticate via VK OAuth2
- ✅ JWT access tokens issued with 15-minute expiry
- ✅ JWT refresh tokens issued with 7-day expiry
- ✅ Token refresh flow works correctly
- ✅ User can logout (token revocation)
- ✅ Password strength validation enforced
- ✅ Email uniqueness validated
- ✅ OAuth accounts linked to existing users

### Non-Functional Requirements

- ✅ API response time < 200ms (P95)
- ✅ Password hashing uses BCrypt (cost 12)
- ✅ JWT tokens signed with HS256 (minimum 256-bit secret)
- ✅ All endpoints have OpenAPI documentation
- ✅ Integration tests cover all critical flows
- ✅ Service runs in Docker container
- ✅ Health checks configured (liveness, readiness)
- ✅ Metrics exposed via Actuator

### Code Quality

- ✅ No TODO comments (everything implemented)
- ✅ No placeholder code
- ✅ All public methods documented
- ✅ Test coverage > 70% for service layer
- ✅ No security vulnerabilities (SQL injection, XSS)
- ✅ Proper error handling everywhere
- ✅ Logging for all important events

---

## Implementation Steps (Prompts)

Phase 1 consists of 5 sequential prompts:

### Prompt 1: Project Structure & Dependencies (45 min)

- Create Gradle multi-module project
- Configure all dependencies
- Setup package structure
- Create Docker and docker-compose files
- Initialize Git repository

### Prompt 2: Database Schema & JPA Entities (60 min)

- Study Kotlin entity models
- Create Java entities with JPA annotations
- Create repositories with custom queries
- Write Flyway migration scripts
- Configure connection pooling

### Prompt 3: Security Configuration & JWT (90 min)

- Implement JWT token service
- Configure Spring Security filter chain
- Implement VK OAuth2 integration
- Create authentication service
- Add password encoding

### Prompt 4: REST Controllers & API (60 min)

- Implement all REST endpoints
- Add request/response DTOs
- Implement global exception handler
- Add OpenAPI documentation
- Configure CORS and rate limiting

### Prompt 5: Docker & Testing (60 min)

- Create production Dockerfile
- Configure docker-compose for local dev
- Write integration tests
- Add health indicators
- Create deployment README

---

## Configuration Requirements

### Environment Variables

Required for local development:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=authdb
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=your-super-secret-jwt-key-change-in-production-min-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=900000     # 15 minutes in ms
JWT_REFRESH_TOKEN_EXPIRATION=604800000 # 7 days in ms

# VK OAuth2
VK_CLIENT_ID=your_vk_application_id
VK_CLIENT_SECRET=your_vk_client_secret
VK_REDIRECT_URI=http://localhost:8080/api/v1/auth/oauth2/callback/vk

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### Application Configuration

Key configurations in `application.yml`:

- Virtual threads enabled
- Database connection pooling (HikariCP)
- JPA/Hibernate settings
- Flyway migrations
- Redis caching
- OAuth2 client registration
- JWT configuration
- Actuator endpoints
- CORS settings
- Logging configuration

---

## Testing Strategy

### Unit Tests

- Service layer business logic
- JWT token generation/validation
- Password encoding/validation
- Entity mapping logic
- Custom validators

### Integration Tests

- REST API endpoints (all CRUD operations)
- Database operations (repositories)
- OAuth2 flow (mock VK provider)
- JWT authentication filter
- Global exception handling

### Test Data

- Use Testcontainers for PostgreSQL
- Use embedded Redis for tests
- Mock OAuth2 providers
- Use in-memory test users

---

## Monitoring & Observability

### Actuator Endpoints

```
GET /actuator/health          - Overall health
GET /actuator/health/liveness - K8s liveness probe
GET /actuator/health/readiness - K8s readiness probe
GET /actuator/info            - Build information
GET /actuator/metrics         - Application metrics
GET /actuator/prometheus      - Prometheus metrics
```

### Custom Health Indicators

- Database connectivity check
- Redis connectivity check
- VK OAuth provider availability

### Metrics to Track

- Authentication success/failure rate
- Token generation rate
- OAuth2 callback success rate
- API response times
- Database connection pool usage

---

## Security Considerations

### Password Security

- BCrypt hashing with cost factor 12
- Minimum password length: 8 characters
- Must contain: uppercase, lowercase, digit
- Password history to prevent reuse (future)

### JWT Security

- Access token: 15-minute expiry
- Refresh token: 7-day expiry, single-use
- Tokens stored in httpOnly cookies (optional)
- Token revocation on logout
- No sensitive data in JWT payload

### OAuth2 Security

- State parameter for CSRF protection
- Validate redirect URI
- Secure token storage (encrypt at rest)
- Token refresh before expiry

### API Security

- Rate limiting: 5 requests/minute for login
- Rate limiting: 100 requests/minute for other endpoints
- CORS configured for frontend domains only
- Input validation on all endpoints
- SQL injection prevention (parameterized queries)

---

## Future Enhancements (Post Phase 1)

These will be implemented in later phases:

- Email verification flow
- Password reset flow
- Two-factor authentication (2FA)
- Social login (Google, Facebook, GitHub)
- Account lockout after failed attempts
- Audit logging for security events
- Role-based access control (RBAC)
- Permission-based authorization
- API key authentication for services

---

## Troubleshooting Guide

### Common Issues

**Issue**: JWT tokens not validating

- Check JWT_SECRET environment variable
- Verify token expiration times
- Check clock synchronization

**Issue**: OAuth2 callback fails

- Verify VK_CLIENT_ID and VK_CLIENT_SECRET
- Check redirect URI matches VK app configuration
- Ensure VK app is in production mode

**Issue**: Database connection fails

- Check PostgreSQL is running: `docker ps`
- Verify DB credentials in environment variables
- Check database exists: `docker exec -it postgres psql -U postgres -l`

**Issue**: Redis connection fails

- Check Redis is running: `docker ps`
- Verify Redis host and port
- Check Redis password if configured

---

## Phase Completion Checklist

Before moving to Phase 2, verify:

### Code Complete

- [ ] All entities created and tested
- [ ] All repositories implemented
- [ ] All services implemented
- [ ] All controllers implemented
- [ ] All configurations added
- [ ] No TODO comments remain

### Tests Passing

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Test coverage > 70%
- [ ] Manual API testing completed

### Documentation

- [ ] OpenAPI/Swagger UI accessible
- [ ] README.md complete with setup instructions
- [ ] API endpoints documented
- [ ] Environment variables documented

### Deployment

- [ ] Docker image builds successfully
- [ ] docker-compose up works
- [ ] All health checks passing
- [ ] Can register and login via API
- [ ] Can authenticate via VK OAuth2
- [ ] Can refresh tokens
- [ ] Can logout

### Ready for Phase 2

- [ ] Auth service running on port 8080
- [ ] PostgreSQL database initialized
- [ ] Redis cache available
- [ ] Frontend can integrate with auth API
- [ ] JWT tokens working correctly

---

## Next Phase Preview

**Phase 2: Content Service** will implement:

- Content data model with multiple types
- Stub recommendation service
- Content retrieval API with pagination
- Redis caching for content
- Integration with auth service for user context

Phase 2 will depend on Phase 1's JWT authentication for protected endpoints.
