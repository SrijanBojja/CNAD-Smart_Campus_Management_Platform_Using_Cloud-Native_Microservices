# ============================================================
# Stage 1: Build Application
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper / build files
COPY pom.xml ./
COPY src ./src

# Build production jar
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# ============================================================
# Stage 2: Production Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root system user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/auth-service-*.jar /app/auth-service.jar

# Expose default port
EXPOSE 8081

# Environment Variables with Defaults
ENV SERVER_PORT=8081 \
    DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=smart_campus_auth \
    DB_USERNAME=smartcampus_user \
    DB_PASSWORD=smartcampus_pass \
    JWT_SECRET="" \
    JWT_EXPIRATION_MS=86400000 \
    FRONTEND_URL=http://localhost:5173

ENTRYPOINT ["java", "-jar", "/app/auth-service.jar"]
