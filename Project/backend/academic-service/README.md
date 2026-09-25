# Academic Service

The **Academic Service** is a core cloud-native microservice of the **Smart Campus Management Platform**. It manages the academic domain, including course degree programs, semester subject modules, course enrollments, student academic grading/records, and timetable class schedules.

---

## 1. Domain Ownership & Architectural Boundary

- **Dedicated Database**: `smart_campus_academic`
- **Tables Owned**:
  - `courses`
  - `subjects`
  - `course_enrollments`
  - `academic_records`
  - `class_schedules`
- **Database-Per-Service Rule**:
  - Academic Service connects **only** to its own database (`smart_campus_academic`).
  - It **never** queries or connects directly to `smart_campus_auth` or `smart_campus_student`.
  - Student IDs (`student_id`) and Faculty user IDs (`faculty_user_id`) are **application-level references** without cross-database foreign key constraints.
  - Cross-service validation of students and faculty occurs exclusively over REST using `StudentServiceClient` and `AuthServiceClient`.

---

## 2. Technology Stack

- **Runtime**: Java 21
- **Framework**: Spring Boot 3.3.4
- **Persistence**: Spring Data JPA / Hibernate (`ddl-auto: validate`)
- **Database & Migrations**: MySQL 8 with Flyway migrations (`V1__create_academic_tables.sql`)
- **Security**: Spring Security (Stateless JWT authentication & RBAC + Object-Level Authorization)
- **API Documentation**: OpenAPI 3 / Swagger (`springdoc-openapi-starter-webmvc-ui`)
- **Containerization**: Multi-stage Docker image (Eclipse Temurin JRE 21 Alpine)

---

## 3. Environment Variables

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SERVER_PORT` | HTTP server listening port | `8083` |
| `DB_HOST` | MySQL database host | `localhost` |
| `DB_PORT` | MySQL database port | `3306` |
| `DB_NAME` | Dedicated database name | `smart_campus_academic` |
| `DB_USERNAME` | Database username | `smartcampus_user` |
| `DB_PASSWORD` | Database user password (Required) | *No default* |
| `JWT_SECRET` | HMAC-SHA256 Secret ($\ge 256$ bits) (Required) | *No default* |
| `JWT_EXPIRATION_MS` | JWT validity period in milliseconds | `86400000` (24 hours) |
| `AUTH_SERVICE_URL` | Base URL of Authentication Service | `http://localhost:8081` |
| `STUDENT_SERVICE_URL` | Base URL of Student Service | `http://localhost:8082` |
| `FRONTEND_URL` | Allowed origin for CORS | `http://localhost:5173` |

---

## 4. REST API Contract

Base URL: `/api/v1`

### Courses (`/api/v1/courses`)
- `GET /api/v1/courses`: List paginated courses (`ADMIN`, `FACULTY`, `STUDENT`)
- `GET /api/v1/courses/{id}`: Get course by ID (`ADMIN`, `FACULTY`, `STUDENT`)
- `POST /api/v1/courses`: Create new course (`ADMIN`)
- `PUT /api/v1/courses/{id}`: Update course details (`ADMIN`)
- `PATCH /api/v1/courses/{id}/status`: Update course status (`ADMIN`)

### Subjects (`/api/v1/subjects`)
- `GET /api/v1/subjects`: List paginated subjects (`ADMIN`, `FACULTY`, `STUDENT`)
- `GET /api/v1/subjects/{id}`: Get subject by ID (`ADMIN`, `FACULTY`, `STUDENT`)
- `POST /api/v1/subjects`: Create new subject (`ADMIN`, `FACULTY`)
- `PUT /api/v1/subjects/{id}`: Update subject details (`ADMIN`, `FACULTY`)
- `DELETE /api/v1/subjects/{id}`: Delete subject (`ADMIN`)

### Enrollments (`/api/v1/enrollments`)
- `GET /api/v1/enrollments`: List paginated enrollments (`ADMIN`, `FACULTY`)
- `GET /api/v1/enrollments/{id}`: Get enrollment by ID (`ADMIN`, `FACULTY`, `STUDENT` - object-level check)
- `POST /api/v1/enrollments`: Enroll student in course (`ADMIN`)
- `PUT /api/v1/enrollments/{id}`: Update enrollment details/status (`ADMIN`)
- `DELETE /api/v1/enrollments/{id}`: Delete enrollment (`ADMIN`)

### Academic Records (`/api/v1/academic-records`)
- `GET /api/v1/academic-records`: List paginated academic records (`ADMIN`, `FACULTY`)
- `GET /api/v1/academic-records/{id}`: Get academic record by ID (`ADMIN`, `FACULTY`, `STUDENT` - object-level check)
- `POST /api/v1/academic-records`: Create academic record / enter marks (`ADMIN`, `FACULTY`)
- `PUT /api/v1/academic-records/{id}`: Update academic record (`ADMIN`, `FACULTY`)

### Class Schedules (`/api/v1/schedules`)
- `GET /api/v1/schedules`: List paginated class schedules (`ADMIN`, `FACULTY`, `STUDENT`)
- `GET /api/v1/schedules/{id}`: Get schedule by ID (`ADMIN`, `FACULTY`, `STUDENT`)
- `POST /api/v1/schedules`: Create timetable schedule (`ADMIN`, `FACULTY`)
- `PUT /api/v1/schedules/{id}`: Update timetable schedule (`ADMIN`, `FACULTY`)
- `DELETE /api/v1/schedules/{id}`: Delete schedule (`ADMIN`, `FACULTY`)

---

## 5. Security & Authorization Model

- **Stateless JWT**: Validates token signature with `${JWT_SECRET}` and extracts `userId`, `username`, `email`, and `roles`.
- **Role-Based Access Control**: Guarded via Spring Security `@PreAuthorize`.
- **Object-Level Authorization**:
  - `GET /api/v1/enrollments/{id}` and `GET /api/v1/academic-records/{id}`:
  - If caller possesses `STUDENT` role, the student ID referenced on the entity is verified against the caller's authenticated user ID via `StudentServiceClient`.
  - Attempts by students to view other students' enrollments or academic records return `403 Forbidden`.

---

## 6. How to Run Locally

### Run Database Migration & Service
```bash
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=your_base64_jwt_secret_at_least_256_bits
mvn spring-boot:run
```

### Run Tests
```bash
mvn clean test
```

### Swagger Documentation
- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

### Actuator Health Endpoint
- Health: `http://localhost:8083/actuator/health`
- Info: `http://localhost:8083/actuator/info`
