# Stage 1: Build
FROM gradle:8.7-jdk21-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first for better caching
COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY common/build.gradle.kts common/
COPY auth-service/build.gradle.kts auth-service/

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY common/src common/src
COPY auth-service/src auth-service/src

# Build the application
RUN ./gradlew :auth-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S appuser && adduser -S appuser -G appuser

WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder --chown=appuser:appuser /app/auth-service/build/libs/auth-service.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# JVM options for container environment with virtual threads enabled
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
