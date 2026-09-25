# Attendance Service - Smart Campus Management Platform

The **Attendance Service** is a core microservice of the Smart Campus Management Platform responsible for scheduling class attendance sessions and tracking student attendance records.

---

## 1. Architectural Role & Responsibilities

- **Attendance Session Management**: Scheduling class lecture/lab sessions (`subjectId`, `facultyUserId`, `sessionDate`, `startTime`, `endTime`, `roomNumber`, `academicYear`, `semester`, `section`).
- **Student Attendance Recording**: Marking and tracking individual student attendance records (`sessionId`, `studentId`, `status`, `markedAt`, `remarks`).
- **Status Lifecycle**: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`.
- **Database-Per-Service Isolation**: The Attendance Service exclusively owns and accesses `smart_campus_attendance`. It never accesses `smart_campus_auth`, `smart_campus_student`, or `smart_campus_academic` directly.
- **Cross-Service Reference Integrity**:
  - `student_id` is an application-level reference validated via `StudentServiceClient` (`GET /api/v1/students/{id}`).
  - `subject_id` is an application-level reference validated via `AcademicServiceClient` (`GET /api/v1/subjects/{id}`).
  - `faculty_user_id` is an application-level reference validated via `AuthServiceClient` (`GET /api/v1/internal/users/{userId}/validation?requiredRole=FACULTY`).

---

## 2. Technology Stack

- **Java**: 21 LTS
- **Framework**: Spring Boot 3.3.4
- **Database**: MySQL 8 (`smart_campus_attendance`)
- **Schema Migration**: Flyway (`V1__create_attendance_tables.sql`, `ddl-auto=validate`)
- **Security**: Spring Security with stateless HMAC-SHA256 JWT authentication
- **Documentation**: OpenAPI 3.0 / Swagger UI (`springdoc-openapi`)
- **Containerization**: Multi-stage Docker build

---

## 3. Database Schema

### `attendance_sessions`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PK, AUTO_INCREMENT` | Session identifier |
| `subject_id` | `BIGINT` | `NOT NULL` | App-level reference to Academic Service |
| `faculty_user_id` | `BIGINT` | `NOT NULL` | App-level reference to Auth Service |
| `session_date` | `DATE` | `NOT NULL` | Date of lecture/session |
| `start_time` | `TIME` | `NOT NULL` | Session start time |
| `end_time` | `TIME` | `NOT NULL` | Session end time |
| `room_number` | `VARCHAR(50)` | `NULL` | Classroom / Hall |
| `academic_year` | `VARCHAR(20)` | `NOT NULL` | E.g. `2026-2027` |
| `semester` | `INT` | `NOT NULL` | Semester number |
| `section` | `VARCHAR(20)` | `NULL` | Class section |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Record creation timestamp |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Record update timestamp |

### `attendance`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PK, AUTO_INCREMENT` | Record identifier |
| `session_id` | `BIGINT` | `FK -> attendance_sessions(id) ON DELETE CASCADE` | Internal foreign key |
| `student_id` | `BIGINT` | `NOT NULL` | App-level reference to Student Service |
| `status` | `VARCHAR(20)` | `NOT NULL` | `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` |
| `marked_at` | `TIMESTAMP` | `NOT NULL` | Timestamp marked |
| `remarks` | `VARCHAR(255)`| `NULL` | Optional remarks |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Record creation timestamp |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Record update timestamp |

**Unique Constraint**: `uk_attendance_session_student (session_id, student_id)`.

---

## 4. REST API Overview

Base path: `/api/v1/attendance`

### Attendance Sessions
| HTTP Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/attendance/sessions` | `ADMIN`, `FACULTY`, `STUDENT` | List sessions (paginated, filter by `subjectId`, `facultyUserId`, `sessionDate`, etc.) |
| `GET` | `/api/v1/attendance/sessions/{id}` | `ADMIN`, `FACULTY`, `STUDENT` | Get session details by ID |
| `POST` | `/api/v1/attendance/sessions` | `ADMIN`, `FACULTY` | Create attendance session |
| `PUT` | `/api/v1/attendance/sessions/{id}` | `ADMIN`, `FACULTY` | Update attendance session |
| `DELETE` | `/api/v1/attendance/sessions/{id}` | `ADMIN`, `FACULTY` | Delete attendance session |

### Attendance Records
| HTTP Method | Path | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/attendance/records` | `ADMIN`, `FACULTY` | List records (paginated, filter by `sessionId`, `studentId`, `status`) |
| `GET` | `/api/v1/attendance/records/{id}` | `ADMIN`, `FACULTY`, `STUDENT` | Get record by ID (Object-level authorization enforced for `STUDENT`) |
| `POST` | `/api/v1/attendance/records` | `ADMIN`, `FACULTY` | Record student attendance |
| `PUT` | `/api/v1/attendance/records/{id}` | `ADMIN`, `FACULTY` | Update attendance record |
| `DELETE` | `/api/v1/attendance/records/{id}` | `ADMIN`, `FACULTY` | Delete attendance record |

---

## 5. Security & Authorization

- **Stateless Bearer JWT Authentication**: Validated on every incoming request.
- **RBAC**:
  - `ADMIN`: Full administrative access.
  - `FACULTY`: Full management of sessions and attendance marking.
  - `STUDENT`: Read access to sessions; object-level access to their own attendance records only.
- **Object-Level Authorization**: When a `STUDENT` requests `GET /api/v1/attendance/records/{id}`, the service resolves `student_id` via `StudentServiceClient` and verifies `student.userId == principal.userId`. Access to another student's record returns `403 Forbidden`.

---

## 6. Environment Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `SERVER_PORT` | Service port | `8084` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `smart_campus_attendance` |
| `DB_USERNAME` | Database username | `smartcampus_user` |
| `DB_PASSWORD` | Database password | *Required in production* |
| `JWT_SECRET` | HMAC-SHA secret ($\ge 256$ bits) | *Required in production* |
| `JWT_EXPIRATION_MS` | Token expiry in ms | `86400000` (24h) |
| `AUTH_SERVICE_URL` | Upstream Auth Service URL | `http://localhost:8081` |
| `STUDENT_SERVICE_URL` | Upstream Student Service URL | `http://localhost:8082` |
| `ACADEMIC_SERVICE_URL` | Upstream Academic Service URL | `http://localhost:8083` |
| `FRONTEND_URL` | Allowed CORS origin | `http://localhost:5173` |

---

## 7. Local Run & Testing

```bash
# Run tests
mvn clean test

# Run application locally
mvn spring-boot:run
```

- **Swagger UI**: `http://localhost:8084/swagger-ui.html`
- **Health Check**: `http://localhost:8084/actuator/health`
