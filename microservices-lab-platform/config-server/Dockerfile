# ============================================================
# Dockerfile — Config Server
# Multi-stage build: Maven build → minimal JRE runtime
# ============================================================

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy parent POM and common-library first for layer caching
COPY pom.xml .
COPY common-library/pom.xml common-library/
COPY common-library/src common-library/src
COPY config-server/pom.xml config-server/

# Download dependencies (cached if POMs unchanged)
RUN mvn dependency:go-offline -pl common-library,config-server -am -q

# Copy source and build
COPY config-server/src config-server/src
RUN mvn clean package -pl common-library,config-server -am -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the fat JAR from builder
COPY --from=builder /workspace/config-server/target/*.jar config-server.jar

# Health check — Eureka takes ~30s to start
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q --spider http://localhost:8888/actuator/health || exit 1

EXPOSE 8888

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "config-server.jar"]
