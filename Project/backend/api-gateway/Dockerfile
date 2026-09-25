# ============================================================
# Stage 1: Build Application
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

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
