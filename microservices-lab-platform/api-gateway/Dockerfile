# ============================================================
# Dockerfile — API Gateway
# ============================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common-library/pom.xml common-library/
COPY common-library/src common-library/src
COPY api-gateway/pom.xml api-gateway/

RUN mvn dependency:go-offline -pl common-library,api-gateway -am -q

COPY api-gateway/src api-gateway/src
RUN mvn clean package -pl common-library,api-gateway -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/api-gateway/target/*.jar api-gateway.jar

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "api-gateway.jar"]
