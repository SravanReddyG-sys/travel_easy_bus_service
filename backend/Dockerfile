# Multi-stage build for TravelEasy Bus Microservice
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper & pom first for dependency caching
COPY pom.xml .
COPY src src

# Build application skipping tests for container image assembly
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root application user for container security
RUN addgroup -S traveleasy && adduser -S traveleasy -G traveleasy
USER traveleasy:traveleasy

# Copy built jar from builder stage
COPY --from=builder /workspace/target/bus-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/v1/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
