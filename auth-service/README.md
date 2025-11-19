# Auth Service

A production-ready authentication microservice built with Java 21 and Spring Boot 3.2. Provides user registration, JWT-based authentication, and OAuth2 (VK) integration.

## Features

- User registration with email/password
- JWT access and refresh token authentication
- OAuth2 authentication (VK)
- Password validation with security requirements
- Rate limiting with Redis
- Health checks (liveness/readiness)
- Kubernetes-ready deployment
- OpenAPI/Swagger documentation

## Technology Stack

- **Java 21** with Virtual Threads
- **Spring Boot 3.2.5**
- **Spring Security 6.x**
- **PostgreSQL 15** with Flyway migrations
- **Redis 7** for caching and rate limiting
- **JJWT 0.12.3** for JWT handling
- **Docker** and **Kubernetes**

## Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Gradle 8.x (or use wrapper)

### Running Locally

1. Clone the repository and navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Copy environment file:
   ```bash
   cp .env.example .env
   ```

3. Start with Docker Compose:
   ```bash
   docker-compose up -d
   ```

4. Access the service:
   - API: http://localhost:8080/api/v1
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health: http://localhost:8080/actuator/health

### Running Without Docker

1. Start PostgreSQL and Redis:
   ```bash
   # PostgreSQL
   docker run -d --name postgres \
     -e POSTGRES_DB=authdb \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 postgres:15-alpine

   # Redis
   docker run -d --name redis -p 6379:6379 redis:7-alpine
   ```

2. Run the application:
   ```bash
   ./gradlew :auth-service:bootRun
   ```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login with credentials |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout (revoke refresh token) |

### OAuth2

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/auth/oauth2/vk/url` | Get VK authorization URL |
| GET | `/api/v1/auth/oauth2/callback/vk` | VK OAuth2 callback |

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/users/me` | Get current user profile |
| PUT | `/api/v1/users/me` | Update current user |
| DELETE | `/api/v1/users/me` | Delete current user |

For detailed API documentation, see [API.md](API.md).

## Project Structure

```
auth-service/
├── src/main/java/com/contentaggregation/auth/
│   ├── config/           # Configuration classes
│   ├── controller/       # REST controllers
│   ├── dto/              # Data transfer objects
│   │   ├── request/
│   │   └── response/
│   ├── entity/           # JPA entities
│   ├── exception/        # Custom exceptions
│   ├── health/           # Health indicators
│   ├── mapper/           # Entity-DTO mappers
│   ├── repository/       # JPA repositories
│   ├── security/         # Security components
│   ├── service/          # Business logic
│   │   └── impl/
│   └── validation/       # Custom validators
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/     # Flyway migrations
├── src/test/             # Tests
├── k8s/                  # Kubernetes manifests
├── Dockerfile
├── API.md
└── DEPLOYMENT.md
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `authdb` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | Base64 JWT secret | (dev default) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL (ms) | `900000` |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL (ms) | `604800000` |

### JWT Secret

The JWT secret must be Base64 encoded and at least 256 bits (32 bytes).

Generate a production secret:
```bash
openssl rand -base64 32
```

## Building

### Build JAR

```bash
./gradlew :auth-service:build
```

### Build Docker Image

```bash
docker build -t contentaggregation/auth-service:latest -f auth-service/Dockerfile .
```

### Run Tests

```bash
# All tests
./gradlew :auth-service:test

# Integration tests only
./gradlew :auth-service:test --tests '*IntegrationTest'

# Unit tests only
./gradlew :auth-service:test --tests '*Test' --exclude-task '*IntegrationTest'
```

## Deployment

### Kubernetes

1. Create secrets:
   ```bash
   kubectl create secret generic auth-service-secrets \
     --from-literal=DB_PASSWORD=your_password \
     --from-literal=JWT_SECRET=your_base64_secret
   ```

2. Apply manifests:
   ```bash
   kubectl apply -f auth-service/k8s/
   ```

For detailed deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md).

## Health Checks

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Full Health**: `/actuator/health`

## Security

- Passwords hashed with BCrypt (strength 12)
- JWT tokens with HS256 signing
- CORS configured for frontend
- Rate limiting per IP (100 req/min)
- Input validation on all endpoints

## Testing

The service includes:

- **Unit Tests**: Service and utility classes
- **Integration Tests**: Full API testing with Testcontainers

Test coverage target: >70%

## Monitoring

### Actuator Endpoints

- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## License

Proprietary - Content Aggregation Platform

## Contributing

1. Follow Google Java Style Guide
2. Write tests for new features
3. Update documentation
4. Run full test suite before committing
