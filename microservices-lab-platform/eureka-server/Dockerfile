# ============================================================
# Dockerfile — Eureka Server
# ============================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common-library/pom.xml common-library/
COPY common-library/src common-library/src
COPY eureka-server/pom.xml eureka-server/

RUN mvn dependency:go-offline -pl common-library,eureka-server -am -q

COPY eureka-server/src eureka-server/src
RUN mvn clean package -pl common-library,eureka-server -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/eureka-server/target/*.jar eureka-server.jar

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD wget -q --spider http://localhost:8761/actuator/health || exit 1

EXPOSE 8761

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "eureka-server.jar"]
