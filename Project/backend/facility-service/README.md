# Facility Service

The **Facility Service** is a core cloud-native microservice of the **Smart Campus Management Platform**. It provides comprehensive lifecycle management for campus physical spaces (classrooms, laboratories, auditoriums, seminar halls, sports complexes, libraries), reservation/booking workflows with conflict detection, and facility maintenance work orders.

---

## Architecture & Responsibilities

- **Service Port**: `8086`
- **Database**: `smart_campus_facility` (MySQL 8)
- **Schema Management**: Flyway database migrations (`V1__create_facility_tables.sql`)
- **Isolation**: Follows strict Database-per-Service principles. External user IDs (`requested_by_user_id`, `approved_by_user_id`, `reported_by_user_id`, `assigned_to_user_id`) remain purely application-level references without direct cross-service database foreign keys.
- **Conflict Management**: Real-time overlapping booking conflict detection ensuring no two `APPROVED` requests occupy the same facility concurrently.
- **Cross-Service Validation**: REST-based integration with `auth-service` (`GET /api/v1/internal/users/{userId}/validation?requiredRole=FACULTY`) with 5-second timeouts and safe error mapping.

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
CREATE TABLE facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    facility_type VARCHAR(50) NOT NULL,
    description TEXT,
    building VARCHAR(150),
    room_number VARCHAR(50),
    capacity INT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_facilities_type (facility_type),
    INDEX idx_facilities_building (building),
    INDEX idx_facilities_status (status)
);

CREATE TABLE facility_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    request_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    purpose VARCHAR(300) NOT NULL,
    status VARCHAR(30) NOT NULL,
    approved_by_user_id BIGINT NULL,
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_facility_request_facility FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE,
    INDEX idx_facility_requests_facility (facility_id),
    INDEX idx_facility_requests_user (requested_by_user_id),
    INDEX idx_facility_requests_date (request_date),
    INDEX idx_facility_requests_status (status)
);

CREATE TABLE maintenance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id BIGINT NOT NULL,
    reported_by_user_id BIGINT NOT NULL,
    assigned_to_user_id BIGINT NULL,
    issue_description TEXT NOT NULL,
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_maintenance_facility FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE,
    INDEX idx_maintenance_facility (facility_id),
    INDEX idx_maintenance_status (status),
    INDEX idx_maintenance_priority (priority)
);
```

---

## API Endpoints

### Facilities (`/api/v1/facilities`)
| Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/facilities` | `ADMIN`, `FACULTY`, `STUDENT` | List paginated facilities with filtering |
| `GET` | `/api/v1/facilities/{id}` | `ADMIN`, `FACULTY`, `STUDENT` | Get facility details by ID |
| `POST` | `/api/v1/facilities` | `ADMIN` | Create new facility |
| `PUT` | `/api/v1/facilities/{id}` | `ADMIN` | Update facility details |
| `PATCH`| `/api/v1/facilities/{id}/status`| `ADMIN` | Update facility availability/status |
| `DELETE`| `/api/v1/facilities/{id}` | `ADMIN` | Safely delete or retire facility |

### Facility Requests (`/api/v1/facilities/{facilityId}/requests`)
| Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/facilities/{facilityId}/requests` | `ADMIN`, `FACULTY`, `STUDENT` | Request facility booking (requester from JWT) |
| `GET` | `/api/v1/facilities/{facilityId}/requests` | `ADMIN`, `FACULTY` | List requests for a facility |
| `GET` | `/api/v1/facilities/{facilityId}/requests/{requestId}` | `ADMIN`, `FACULTY`, `STUDENT` (own) | View single request details |
| `PUT` | `/api/v1/facilities/{facilityId}/requests/{requestId}` | `ADMIN`, `FACULTY`, `STUDENT` (own/pending) | Update booking details |
| `PATCH`| `/api/v1/facilities/{facilityId}/requests/{requestId}/status` | `ADMIN`, `FACULTY` | Approve/Reject/Cancel status transition |
| `DELETE`| `/api/v1/facilities/{facilityId}/requests/{requestId}` | `ADMIN`, `FACULTY`, `STUDENT` (own) | Cancel booking request |

### Facility Maintenance (`/api/v1/facilities/{facilityId}/maintenance`)
| Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/facilities/{facilityId}/maintenance` | `ADMIN`, `FACULTY` | Report maintenance issue (reporter from JWT) |
| `GET` | `/api/v1/facilities/{facilityId}/maintenance` | `ADMIN`, `FACULTY` | List maintenance work orders |
| `GET` | `/api/v1/facilities/{facilityId}/maintenance/{maintenanceId}` | `ADMIN`, `FACULTY` | Get maintenance record details |
| `PUT` | `/api/v1/facilities/{facilityId}/maintenance/{maintenanceId}` | `ADMIN`, `FACULTY` | Update maintenance details |
| `PATCH`| `/api/v1/facilities/{facilityId}/maintenance/{maintenanceId}/status` | `ADMIN`, `FACULTY` | Advance maintenance lifecycle status |
| `DELETE`| `/api/v1/facilities/{facilityId}/maintenance/{maintenanceId}` | `ADMIN`, `FACULTY` | Cancel maintenance work order |

---

## Environment Variables

| Variable | Default | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8086` | Application HTTP port |
| `DB_HOST` | `localhost` | MySQL database host |
| `DB_PORT` | `3306` | MySQL database port |
| `DB_NAME` | `smart_campus_facility` | MySQL database name |
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
