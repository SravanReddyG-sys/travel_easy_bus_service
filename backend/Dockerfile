# Production runtime image for TravelEasy Bus Microservice
# Optimized for CI/CD: Copies pre-tested, packaged JAR from Jenkins workspace
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root application user for container security
RUN addgroup -S traveleasy && adduser -S traveleasy -G traveleasy
USER traveleasy:traveleasy

# Copy verified and tested JAR artifact produced by Jenkins CI
COPY target/bus-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/v1/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
