FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY common common
COPY auth-service auth-service
COPY content-service content-service
COPY metrics-service metrics-service

RUN chmod +x gradlew \
    && ./gradlew :metrics-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S metrics && adduser -S metrics -G metrics
WORKDIR /app

COPY --from=builder /app/metrics-service/build/libs/metrics-service.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8082
USER metrics

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

