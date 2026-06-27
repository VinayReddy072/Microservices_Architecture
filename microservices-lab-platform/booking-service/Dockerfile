# ============================================================
# Dockerfile — Booking Service
# ============================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common-library/pom.xml common-library/
COPY common-library/src common-library/src
COPY booking-service/pom.xml booking-service/

RUN mvn dependency:go-offline -pl common-library,booking-service -am -q

COPY booking-service/src booking-service/src
RUN mvn clean package -pl common-library,booking-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/booking-service/target/*.jar booking-service.jar

HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=5 \
    CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "booking-service.jar"]
