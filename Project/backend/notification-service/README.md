# Notification Service

The **Notification Service** is a core cloud-native microservice of the **Smart Campus Management Platform**. It provides message dispatching, user notification retrieval, unread tracking, read/unread state updates, and recipient verification for academic, attendance, event, and facility announcements.

---

## Architecture & Responsibilities

- **Service Port**: `8087`
- **Database**: `smart_campus_notification` (MySQL 8)
- **Schema Management**: Flyway database migrations (`V1__create_notification_tables.sql`)
- **Isolation**: Follows strict Database-per-Service principles. `recipient_user_id` and domain references (`reference_id`, `reference_type`) remain purely application-level identifiers with zero cross-database foreign keys.
- **Recipient Validation**: REST-based integration with `auth-service` (`GET /api/v1/internal/users/{userId}/validation`) with 5-second timeouts and safe error mapping.
- **Object-Level Authorization**: Enforces strict server-side boundary checks where regular campus users can only retrieve, update, and delete their own notifications.

---

## Technology Stack

- **Java**: 21
- **Framework**: Spring Boot 3.3.4
- **Persistence**: Spring Data JPA / Hibernate (`ddl-auto=validate`)
- **Database**: MySQL 8 / Flyway
- **Security**: Spring Security 6 with stateless JWT Bearer token authentication
- **Documentation**: OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
- **Observability**: Spring Boot Actuator (`/actuator/health`, `/actuator/info`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 in-memory DB

---

## Database Schema

```sql
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notifications_recipient (recipient_user_id),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_created_at (created_at),
    INDEX idx_notifications_recipient_status_created (recipient_user_id, status, created_at)
);
```

---

## API Endpoints

### Notifications (`/api/v1/notifications`)
| Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/notifications` | `ADMIN`, `FACULTY`, `STUDENT` | List paginated notifications for the authenticated user |
| `GET` | `/api/v1/notifications/unread-count` | `ADMIN`, `FACULTY`, `STUDENT` | Get unread notification count for the authenticated user |
| `GET` | `/api/v1/notifications/{id}` | `ADMIN`, `FACULTY`, `STUDENT` (own) | Get single notification by ID |
| `POST` | `/api/v1/notifications` | `ADMIN`, `FACULTY` | Dispatch a new notification to a verified user |
| `PATCH`| `/api/v1/notifications/{id}/read` | `ADMIN`, `FACULTY`, `STUDENT` (own) | Mark notification as READ |
| `PATCH`| `/api/v1/notifications/{id}/unread` | `ADMIN`, `FACULTY`, `STUDENT` (own) | Mark notification as UNREAD |
| `DELETE`| `/api/v1/notifications/{id}` | `ADMIN`, `FACULTY`, `STUDENT` (own) | Delete notification |

---

## Environment Variables

| Variable | Default | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8087` | Application HTTP port |
| `DB_HOST` | `localhost` | MySQL database host |
| `DB_PORT` | `3306` | MySQL database port |
| `DB_NAME` | `smart_campus_notification` | MySQL database name |
| `DB_USERNAME` | `smartcampus_user` | MySQL database username |
| `DB_PASSWORD` | *(required)* | MySQL database password |
| `JWT_SECRET` | *(required)* | 256-bit Base64/raw secret for HS256 validation |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiration time in milliseconds |
| `AUTH_SERVICE_URL` | `http://localhost:8081` | URL for Auth Service recipient validation |
| `FRONTEND_URL` | `http://localhost:5173` | Allowed CORS origin |

---

## Local Development & Testing

```bash
# Run tests
mvn clean test

# Run application locally
mvn spring-boot:run
```
