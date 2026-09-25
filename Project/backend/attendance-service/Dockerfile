# ============================================================
# Stage 1: Build Application
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy build descriptor and source code
COPY pom.xml ./
COPY src ./src

# Build application package without running tests
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

COPY --from=builder /app/target/attendance-service-*.jar /app/attendance-service.jar

# Expose default port
EXPOSE 8084

# Environment Variables
ENV SERVER_PORT=8084 \
    DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=smart_campus_attendance \
    DB_USERNAME=smartcampus_user \
    DB_PASSWORD="" \
    JWT_SECRET="" \
    JWT_EXPIRATION_MS=86400000 \
    AUTH_SERVICE_URL=http://auth-service:8081 \
    STUDENT_SERVICE_URL=http://student-service:8082 \
    ACADEMIC_SERVICE_URL=http://academic-service:8083 \
    FRONTEND_URL=http://localhost:5173

ENTRYPOINT ["java", "-jar", "/app/attendance-service.jar"]
