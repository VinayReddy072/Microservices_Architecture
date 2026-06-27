# 🐳 Docker & Docker Compose Guide

This guide provides a comprehensive walkthrough of how Docker and Docker Compose are used to package, orchestrate, and run the **University Lab Equipment Booking & Maintenance Platform**.

---

## 📋 Table of Contents
1. [Prerequisites](#1-prerequisites)
2. [Deployment Architecture](#2-deployment-architecture)
3. [Understanding the Dockerfiles](#3-understanding-the-dockerfiles)
4. [Step-by-Step Execution Guide](#4-step-by-step-execution-guide)
5. [Managing the Stack (Cheatsheet)](#5-managing-the-stack-cheatsheet)
6. [Simulating Failures for Resilience Demonstration](#6-simulating-failures-for-resilience-demonstration)
7. [Inspecting Infrastructure Containers](#7-inspecting-infrastructure-containers)
8. [Troubleshooting Common Issues](#8-troubleshooting-common-issues)

---

## 1. Prerequisites

Before running the application, make sure you have the following installed on your machine:
* **Docker Desktop** (version 4.x+ recommended)
* **Git** (if pulling code)
* *Optional:* **Java 21** and **Maven 3.9+** (only needed if running services locally outside of Docker)

---

## 2. Deployment Architecture

The application is structured into **9 distinct containers** running inside a shared virtual network. This isolation mirrors a production microservices deployment.

```
                  ┌────────────────────────────────────────┐
                  │          👤 Client / Frontend          │
                  └───────────────────┬────────────────────┘
                                      │ (HTTP:8080)
                                      ▼
                  ┌────────────────────────────────────────┐
                  │            🚪 API Gateway              │
                  └──────┬──────────────────────────┬──────┘
                         │ (HTTP / OpenFeign)       │ (HTTP / OpenFeign)
                         ▼                          ▼
        ┌────────────────────────────────┐  ┌────────────────────────────────┐
        │       📋 Booking Service       │  │     🔬 Equipment Service       │
        │             :8081              │  │             :8082              │
        └──────┬──────────────────┬──────┘  └──────┬──────────────────┬──────┘
               │ (JPA)            │                │ (JPA)            │
               ▼                  │                ▼                  │
        ┌──────────────┐          │         ┌──────────────┐          │
        │ 🐘 booking_db│          │         │ 🐘 equip_db  │          │
        │    :5432     │          │         │    :5433     │          │
        └──────────────┘          │         └──────────────┘          │
                                  │ (AMQP:5672)                       │ (AMQP:5672)
                                  └───────────────┐ ┌─────────────────┘
                                                  ▼ ▼
                                            ┌──────────────┐
                                            │  🐰 RabbitMQ │
                                            │    :15672    │
                                            └──────────────┘
                                                    
  [Supporting Services]:
  - ⚙️ Config Server (:8888)  - 🔍 Eureka Registry (:8761)  - 🔭 Zipkin Tracing (:9411)
```

### Virtual Network & Hostname Resolution
All containers join a custom bridge network called `lab-platform-network`. Inside this network:
* **DNS Resolution:** Containers resolve each other by their service names defined in [docker-compose.yml](file:///c:/Users/SAI%20VIKRANTH%20TEJ/Desktop/Microservices_Architecture/microservices-lab-platform/docker-compose.yml) (e.g. `booking-service` communicates with RabbitMQ using the hostname `rabbitmq` instead of `localhost`).
* **Port Mapping:** Services map their ports to the host machine (e.g. `8080:8080`) so that you can access them from your browser or Postman on `localhost`.

---

## 3. Understanding the Dockerfiles

Each microservice contains a **multi-stage Dockerfile** to minimize the size of the final production runtime images and speed up build caching. 

For example, look at the Dockerfile structure in [booking-service/Dockerfile](file:///c:/Users/SAI%20VIKRANTH%20TEJ/Desktop/Microservices_Architecture/microservices-lab-platform/booking-service/Dockerfile):

```dockerfile
# Stage 1: Build compilation environment
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
COPY common-library/pom.xml common-library/
COPY common-library/src common-library/src
COPY booking-service/pom.xml booking-service/
RUN mvn dependency:go-offline -pl common-library,booking-service -am -q
COPY booking-service/src booking-service/src
RUN mvn clean package -pl common-library,booking-service -am -DskipTests -q

# Stage 2: Minimalist runtime environment
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /workspace/booking-service/target/*.jar booking-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "booking-service.jar"]
```

### Key Highlights:
1. **Compilation Isolation:** The compilation uses JDK 21. Even if your host machine runs a newer version like Java 25, Docker isolates the compile process to JDK 21, avoiding Lombok version mismatch errors.
2. **Layer Caching:** Standard Maven dependencies are fetched (`go-offline`) and cached *before* copying the application source code. This means if you change a Java file, Docker will rebuild instantly without re-downloading the internet.
3. **Security:** A non-root user (`appuser`) is created in Stage 2 to run the JAR file, adhering to security best practices.

Other service Dockerfiles:
* [equipment-service/Dockerfile](file:///c:/Users/SAI%20VIKRANTH%20TEJ/Desktop/Microservices_Architecture/microservices-lab-platform/equipment-service/Dockerfile)
* [api-gateway/Dockerfile](file:///c:/Users/SAI%20VIKRANTH%20TEJ/Desktop/Microservices_Architecture/microservices-lab-platform/api-gateway/Dockerfile)
* [config-server/Dockerfile](file:///c:/Users/SAI%20VIKRANTH%20TEJ/Desktop/Microservices_Architecture/microservices-lab-platform/config-server/Dockerfile)
* [eureka-server/Dockerfile](file:///c:/Users/SAI%20VIKRANTH%20TEJ/Desktop/Microservices_Architecture/microservices-lab-platform/eureka-server/Dockerfile)

---

## 4. Step-by-Step Execution Guide

Follow these steps to launch the entire ecosystem:

### Step 4.1: Prepare Environment Settings
Copy the `.env.example` file to `.env`:
```bash
cp .env.example .env
```
*(Windows PowerShell: `copy .env.example .env`)*

This file contains database credentials, RabbitMQ passwords, and the encryption `JWT_SECRET` key which are securely fed into your containers at startup.

### Step 4.2: Build and Launch the Stack
Run the following command to compile the Java modules inside Docker and launch the containers in the background:
```bash
docker-compose up -d --build
```

### Step 4.3: Monitor Startup Progression
Since services depend on each other, startup ordering is controlled via health checks. Monitor the services booting up:
```bash
docker-compose logs -f
```
Wait about **1.5 to 2 minutes** until you see the microservices successfully registering with Eureka. You can check container statuses with:
```bash
docker-compose ps
```

All containers must display `Up` or `Up (healthy)`.

---

## 5. Managing the Stack (Cheatsheet)

Here is a list of commands to manage the environment:

| Goal | Command |
| :--- | :--- |
| **Stop all containers** | `docker-compose down` |
| **Stop and destroy all data volumes** | `docker-compose down -v` |
| **Rebuild & restart a single service** | `docker-compose up -d --build booking-service` |
| **View logs for a single service** | `docker-compose logs -f booking-service` |
| **Restart the entire stack** | `docker-compose restart` |
| **View real-time resource utilization** | `docker stats` |

---

## 6. Simulating Failures for Resilience Demonstration

For testing and presenting **Resilience4J** resilience features (Circuit Breaker, Retries, and Fallbacks), you can intentionally crash/stop a service.

### Step 6.1: Simulate Equipment Service Downtime
1. Ensure the platform is running and you are logged in.
2. In your terminal, stop the Equipment Service:
   ```bash
   docker-compose stop equipment-service
   ```
3. Open the demo frontend ([index.html](file:///c:/Users/SAI%20VIKRANTH%20TEJ/Desktop/Microservices_Architecture/microservices-lab-platform/frontend/index.html)) and try to create a new booking on the **Bookings** tab.
4. **Resilience behavior in action:**
   * The `booking-service` will attempt to call the `equipment-service` via OpenFeign.
   * After 3 failed retries, the **Resilience4J Circuit Breaker** opens, and the fallback method (`EquipmentClientFallback`) triggers.
   * You will receive a clean **503 Service Unavailable** error in the response area, stating: *"Equipment service is temporarily unavailable. Cannot verify equipment availability..."* instead of a system crash.

### Step 6.2: Recover the Service
Bring the Equipment Service back online:
```bash
docker-compose start equipment-service
```
After it boots up and registers with Eureka (~30 seconds), try booking again. The circuit breaker will transition back to `CLOSED` and bookings will succeed.

---

## 7. Inspecting Infrastructure Containers

Docker maps utility management consoles to your local machine.

### 7.1: PostgreSQL Databases
You can connect to the isolated PostgreSQL databases using any DB Client (like DBeaver, pgAdmin, or VS Code SQL tools) using these coordinates:

* **Booking Database (`booking_db`):**
  * **URL/Host:** `localhost` (Port: `5432`)
  * **Database Name:** `booking_db`
  * **User / Password:** `bookinguser` / `bookingpass`

* **Equipment Database (`equipment_db`):**
  * **URL/Host:** `localhost` (Port: `5433` - *mapped from 5432 to avoid collision*)
  * **Database Name:** `equipment_db`
  * **User / Password:** `equipmentuser` / `equipmentpass`

Alternatively, execute SQL queries directly using Docker:
```bash
docker exec -it postgres-booking psql -U bookinguser -d booking_db -c "SELECT * FROM bookings;"
```

### 7.2: RabbitMQ Broker Dashboard
Navigate to [http://localhost:15672](http://localhost:15672) in your browser:
* **Username / Password:** `guest` / `guest`
* Use this interface to view the `booking.events` topic exchange, see active queues (`equipment.booking.queue`), and track failed messages inside the DLQ (`booking.dlq`).

### 7.3: Zipkin Tracing Dashboard
Navigate to [http://localhost:9411](http://localhost:9411):
* Query traces by `traceId` or service name to view visual call hierarchies, tracking trace propagation from the API Gateway down to Feign calls and async consumer processing.

---

## 8. Troubleshooting Common Issues

### 8.1: Port Conflict (e.g. `Bind for 0.0.0.0:5432 failed`)
* **Cause:** You have a local PostgreSQL or database instance running on your host machine on port 5432.
* **Solution:** Either shut down your local PostgreSQL service (on Windows: open `services.msc`, find PostgreSQL, right-click, and select "Stop"), or modify the port mapping for `postgres-booking` in `docker-compose.yml` to something else (e.g. `"5434:5432"`).

### 8.2: Out of Memory / Containers Crash Silently
* **Cause:** Docker Desktop does not have enough resources allocated to run 9 containers simultaneously.
* **Solution:** In Docker Desktop:
  1. Go to **Settings** -> **Resources** -> **Advanced**.
  2. Increase memory limit to at least **4.0 GB** or **6.0 GB**.
  3. Increase CPUs to at least **4**.
  4. Click "Apply & Restart".

### 8.3: Configuration Changes Not Reflecting
* **Cause:** The `config-server` caches native configuration files inside the image, or you changed a service configuration but haven't refreshed.
* **Solution:** Restart the config server to clear native caches:
  ```bash
  docker-compose restart config-server
  ```
  If you changed code, make sure to rebuild using `--build`:
  ```bash
  docker-compose up -d --build <service-name>
  ```
