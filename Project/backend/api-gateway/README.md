# API Gateway (`api-gateway`)

Centralized Edge Gateway and Routing Layer for the **Smart Campus Management Platform**.

## 1. Purpose & Responsibilities

- **Centralized Entry Point**: Serves as the single edge gateway for all client traffic on port `8080`.
- **Dynamic Routing**: Dispatches incoming requests to downstream microservices.
- **Actuator Health & Observability**: Observability endpoints exposing health checks (with Kubernetes liveness/readiness probes) and application info.

---

## 2. Technology Stack

- **Java 21 (LTS)**
- **Spring Boot 3.3.4**
- **Spring Cloud Gateway (2023.0.3)**
- **Spring Boot Actuator**
- **Docker**

---

## 3. Environment Configuration

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `SERVER_PORT` | Gateway listening port | `8080` |
| `AUTH_SERVICE_URL` | Upstream Auth Service base URL | `http://localhost:8081` |
| `STUDENT_SERVICE_URL` | Upstream Student Service base URL | `http://localhost:8082` |
| `ACADEMIC_SERVICE_URL` | Upstream Academic Service base URL | `http://localhost:8083` |
| `FRONTEND_URL` | Allowed origin for frontend client CORS | `http://localhost:5173` |

---

## 4. Routing Architecture

### Active Routes
| Route ID | Path Predicate | Target Destination |
| :--- | :--- | :--- |
| `auth-service-route` | `/api/v1/auth/**` | `${AUTH_SERVICE_URL}` (`http://localhost:8081`) |
| `student-service-route` | `/api/v1/students/**` | `${STUDENT_SERVICE_URL}` (`http://localhost:8082`) |
| `academic-service-courses-route` | `/api/v1/courses/**` | `${ACADEMIC_SERVICE_URL}` (`http://localhost:8083`) |
| `academic-service-subjects-route` | `/api/v1/subjects/**` | `${ACADEMIC_SERVICE_URL}` (`http://localhost:8083`) |
| `academic-service-enrollments-route` | `/api/v1/enrollments/**` | `${ACADEMIC_SERVICE_URL}` (`http://localhost:8083`) |
| `academic-service-records-route` | `/api/v1/academic-records/**` | `${ACADEMIC_SERVICE_URL}` (`http://localhost:8083`) |
| `academic-service-schedules-route` | `/api/v1/schedules/**` | `${ACADEMIC_SERVICE_URL}` (`http://localhost:8083`) |
| `attendance-service-route` | `/api/v1/attendance/**` | `${ATTENDANCE_SERVICE_URL}` (`http://localhost:8084`) |
| `event-service-route` | `/api/v1/events/**` | `${EVENT_SERVICE_URL}` (`http://localhost:8085`) |

### Planned Downstream Microservices Routing
| Path Predicate | Target Service |
| :--- | :--- |
| `/api/v1/facilities/**` | `facility-service` |
| `/api/v1/notifications/**` | `notification-service` |

---

## 5. Build & Execution

### Run Tests
```bash
mvn clean test
```

### Package JAR
```bash
mvn clean package
```

### Run Locally
```bash
java -jar target/api-gateway-1.0.0.jar
```

---

## 6. Docker

### Build Image
```bash
docker build -t smartcampus/api-gateway:1.0.0 .
```

### Run Container
```bash
docker run -d -p 8080:8080 \
  -e AUTH_SERVICE_URL=http://auth-service:8081 \
  -e FRONTEND_URL=http://localhost:5173 \
  smartcampus/api-gateway:1.0.0
```
