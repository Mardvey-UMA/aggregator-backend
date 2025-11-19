# Content Aggregation Backend

A microservices-based backend for content aggregation platform, built with Java 21 and Spring Boot 3.2+.

## Project Structure

```
content-aggregation-backend/
├── common/                    # Shared DTOs, exceptions, utilities
├── auth-service/             # Authentication & authorization service
├── docker-compose.yml        # Local development stack
├── settings.gradle.kts       # Multi-module configuration
└── build.gradle.kts          # Root build configuration
```

## Technology Stack

- **Java**: 21 (with Virtual Threads enabled)
- **Spring Boot**: 3.2.5
- **Spring Security**: 6.x with OAuth2
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Migration**: Flyway
- **Build**: Gradle (Kotlin DSL)
- **Container**: Docker

## Prerequisites

- Java 21 (JDK)
- Gradle 8.7+
- Docker & Docker Compose
- PostgreSQL 15 (or use Docker)
- Redis 7 (or use Docker)

## Quick Start

### 1. Clone and Setup

```bash
# Clone the repository
git clone <repository-url>
cd content-aggregation-backend

# Copy environment template
cp .env.example .env

# Edit .env with your configuration
nano .env
```

### 2. Run with Docker Compose (Recommended)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service

# Stop services
docker-compose down
```

### 3. Run Locally (Development)

```bash
# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Build the project
./gradlew build

# Run auth-service
./gradlew :auth-service:bootRun
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | authdb |
| `DB_USERNAME` | Database username | postgres |
| `DB_PASSWORD` | Database password | postgres |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `JWT_SECRET` | JWT signing secret (256+ bits) | - |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL (ms) | 900000 |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL (ms) | 604800000 |
| `VK_CLIENT_ID` | VK OAuth2 client ID | - |
| `VK_CLIENT_SECRET` | VK OAuth2 client secret | - |

### Spring Profiles

- `dev` - Development (default)
- `prod` - Production

## API Documentation

Once the service is running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## API Endpoints

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | User registration |
| POST | `/api/v1/auth/login` | User login |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| GET | `/api/v1/auth/oauth2/vk/authorize` | VK OAuth2 authorization |
| GET | `/api/v1/auth/oauth2/vk/callback` | VK OAuth2 callback |

### Protected Endpoints (Require JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/logout` | User logout |
| GET | `/api/v1/auth/me` | Get current user |
| PUT | `/api/v1/auth/me` | Update current user |
| DELETE | `/api/v1/auth/me` | Delete account |

### Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Overall health |
| `/actuator/health/liveness` | Kubernetes liveness |
| `/actuator/health/readiness` | Kubernetes readiness |
| `/actuator/info` | Build information |
| `/actuator/metrics` | Application metrics |

## Development

### Build Commands

```bash
# Clean and build all modules
./gradlew clean build

# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Run specific module tests
./gradlew :auth-service:test

# Check dependencies
./gradlew dependencies
```

### Code Quality

```bash
# Run checkstyle (if configured)
./gradlew checkstyleMain

# Generate test coverage report
./gradlew jacocoTestReport
```

## Docker

### Build Image

```bash
# Build auth-service image
docker build -t auth-service -f auth-service/Dockerfile .
```

### Run Containers

```bash
# Start all services
docker-compose up -d

# Start specific service
docker-compose up -d auth-service

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Database

### Migrations

Flyway migrations are located in:
```
auth-service/src/main/resources/db/migration/
```

Naming convention: `V{version}__{description}.sql`

### Connect to Database

```bash
# Via Docker
docker exec -it auth-postgres psql -U postgres -d authdb

# Via local psql
psql -h localhost -U postgres -d authdb
```

## Testing

### Unit Tests

```bash
./gradlew test
```

### Integration Tests

Integration tests use Testcontainers:

```bash
./gradlew integrationTest
```

## Troubleshooting

### Common Issues

**Issue**: JWT tokens not validating
- Check JWT_SECRET environment variable
- Verify token expiration times
- Check clock synchronization

**Issue**: Database connection fails
- Check PostgreSQL is running: `docker ps`
- Verify DB credentials in .env
- Check database exists

**Issue**: Redis connection fails
- Check Redis is running: `docker ps`
- Verify Redis host and port

### Logs

```bash
# Docker logs
docker-compose logs auth-service

# Application logs (local)
tail -f auth-service/logs/application.log
```

## Security

- JWT tokens for stateless authentication
- BCrypt (cost 12) for password hashing
- OAuth2 for VK social login
- HTTPS required in production
- Input validation on all endpoints

## License

MIT License

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request
