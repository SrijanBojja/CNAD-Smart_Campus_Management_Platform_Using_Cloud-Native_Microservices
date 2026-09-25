# Event Service

The **Event Service** is a core cloud-native microservice of the **Smart Campus Management Platform**. It provides comprehensive scheduling, lifecycle management, attendee capacity management, and participant registration for campus events, workshops, seminars, and extracurricular activities.

---

## Architecture & Responsibilities

- **Service Port**: `8085`
- **Database**: `smart_campus_event` (MySQL 8)
- **Schema Management**: Flyway database migrations (`V1__create_event_tables.sql`)
- **Isolation**: Follows Database-per-Service principles. Stores cross-service references (`organizer_user_id`, `user_id`) strictly as application-level identifiers without direct database-level foreign keys across services.
- **Cross-Service Validation**: Validates event organizers and roles via REST communication with `auth-service` (`GET /api/v1/internal/users/{userId}/validation?requiredRole=FACULTY`).

---

## Technology Stack

- **Java**: 21
- **Framework**: Spring Boot 3.3.4
- **Persistence**: Spring Data JPA / Hibernate (with `ddl-auto=validate`)
- **Database**: MySQL 8 / Flyway
- **Security**: Spring Security 6 with stateless JWT Bearer token authentication
- **Documentation**: OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
- **Observability**: Spring Boot Actuator (`/actuator/health`, `/actuator/info`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 in-memory DB

---

## Database Schema

```sql
CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(50) NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    location VARCHAR(200),
    organizer_user_id BIGINT NOT NULL,
    capacity INT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE event_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_event_user UNIQUE (event_id, user_id),
    CONSTRAINT fk_event_registration FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);
```

---

## API Endpoints

### Event Management (`/api/v1/events`)
| Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/events` | `ADMIN`, `FACULTY`, `STUDENT` | List paginated events with filtering |
| `GET` | `/api/v1/events/{id}` | `ADMIN`, `FACULTY`, `STUDENT` | Get event details by ID |
| `POST` | `/api/v1/events` | `ADMIN`, `FACULTY` | Schedule a new event |
| `PUT` | `/api/v1/events/{id}` | `ADMIN`, `FACULTY` (organizer) | Update event details |
| `DELETE`| `/api/v1/events/{id}` | `ADMIN`, `FACULTY` (organizer) | Cancel or delete event |

### Event Registrations (`/api/v1/events/{eventId}/registrations`)
| Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/events/{eventId}/registrations` | `ADMIN`, `FACULTY`, `STUDENT` | Register authenticated user |
| `GET` | `/api/v1/events/{eventId}/registrations` | `ADMIN`, `FACULTY` | List event registrations |
| `GET` | `/api/v1/events/{eventId}/registrations/{registrationId}` | `ADMIN`, `FACULTY`, `STUDENT` (own) | View registration details |
| `DELETE`| `/api/v1/events/{eventId}/registrations/{registrationId}` | `ADMIN`, `FACULTY` (organizer), `STUDENT` (own) | Cancel registration |

---

## Environment Variables

| Variable | Default | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8085` | Application HTTP port |
| `DB_HOST` | `localhost` | MySQL database host |
| `DB_PORT` | `3306` | MySQL database port |
| `DB_NAME` | `smart_campus_event` | MySQL database name |
| `DB_USERNAME` | `smartcampus_user` | MySQL database username |
| `DB_PASSWORD` | *(required)* | MySQL database password |
| `JWT_SECRET` | *(required)* | 256-bit Base64/raw secret for HS256 validation |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiration time in milliseconds |
| `AUTH_SERVICE_URL` | `http://localhost:8081` | URL for Auth Service user validation |
| `FRONTEND_URL` | `http://localhost:5173` | Allowed CORS origin |

---

## Local Development & Testing

```bash
# Run tests
mvn clean test

# Run application locally
mvn spring-boot:run
```
