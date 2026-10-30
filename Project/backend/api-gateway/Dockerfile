# ============================================================
# Stage 1: Build Application
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests

# ============================================================
# Stage 2: Production Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/api-gateway-*.jar /app/api-gateway.jar

EXPOSE 8080

ENV SERVER_PORT=8080 \
    AUTH_SERVICE_URL=http://localhost:8081 \
    FRONTEND_URL=http://localhost:5173

ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]
