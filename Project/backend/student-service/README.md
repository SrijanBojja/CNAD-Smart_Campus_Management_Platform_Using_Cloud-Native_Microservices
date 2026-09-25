# Student Service (`student-service`)

Cloud-Native Student Domain Microservice for the **Smart Campus Management Platform**.

## 1. Purpose & Responsibilities

- **Student Profile Management**: Complete lifecycle handling of student enrollment profiles, departments, degree programs, and academic cohorts.
- **Academic Identity & Separation of Concerns**: Manages student profile details independently from user authentication.
- **Cross-Service Validation**: Validates user identity and role with `auth-service` via internal REST API before creating student profiles.
- **Object-Level Authorization**: Enforces strict profile access boundaries (e.g., `STUDENT` users can view only their own record; `ADMIN` and `FACULTY` can access permitted records).
- **Database Ownership**: Exclusively owns the `smart_campus_student` MySQL database with Flyway schema migration.

---

## 2. Architectural Boundaries & Cross-Service Integration

1. **Database per Service**:
   - `student-service` exclusively owns `smart_campus_student`.
   - `student-service` **never** directly connects to or queries `smart_campus_auth`.
2. **Application-Level User Reference**:
   - `students.user_id` is an application-level reference to the Auth user's primary key, not a foreign key across databases.
3. **REST-Driven Identity Validation**:
   - During student creation (`POST /api/v1/students`), `student-service` communicates synchronously via HTTP with `auth-service`:
     ```http
     GET /api/v1/internal/users/{userId}/validation?requiredRole=STUDENT
     Authorization: Bearer <ADMIN_JWT>
     ```
   - Only if `active == true` and `hasRequiredRole == true` is the student profile persisted.

---

## 3. Technology Stack

- **Java 21 (LTS)**
- **Spring Boot 3.3.4**
- **Spring Data JPA & Hibernate**
- **Spring Security (Stateless JWT)**
- **Flyway** (database migrations)
- **MySQL 8** (runtime database)
- **JJWT 0.12.6** (JWT token parsing)
- **SpringDoc OpenAPI 2.6.0 / Swagger UI**
- **Jakarta Bean Validation**
- **JUnit 5 & Mockito** (testing)
- **Docker** (containerization)

---

## 4. Environment Configuration

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `SERVER_PORT` | Port the service listens on | `8082` |
| `DB_HOST` | MySQL host address | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Student database name | `smart_campus_student` |
| `DB_USERNAME` | Non-root database username | `smartcampus_user` |
| `DB_PASSWORD` | Database user password | *(Required - No default)* |
| `JWT_SECRET` | 256-bit secret key for HMAC-SHA256 | *(Required - No default)* |
| `JWT_EXPIRATION_MS` | JWT expiration time in milliseconds | `86400000` (24 hours) |
| `AUTH_SERVICE_URL` | Base URL of upstream Auth Service | `http://localhost:8081` |
| `FRONTEND_URL` | Allowed origin for CORS | `http://localhost:5173` |

> **Security Note**: `DB_PASSWORD` and `JWT_SECRET` are mandatory environment variables and must be supplied through the runtime environment. No default passwords or secrets are configured.

---

## 5. Database Schema & Flyway Migration

Flyway owns all schema migrations located in `src/main/resources/db/migration/`:

- `V1__create_student_tables.sql`:
  - `students`: `id` (PK), `user_id` (Unique), `student_number` (Unique), `first_name`, `last_name`, `date_of_birth`, `phone`, `department`, `program`, `year_of_study`, `section`, `status`, `created_at`, `updated_at`.

---

## 6. REST API Endpoints

Base path: `/api/v1/students`

### 1. List Students (Paginated)
* **Endpoint**: `GET /api/v1/students?page=0&size=20&sort=createdAt,desc`
* **Authorization**: `ADMIN`, `FACULTY`
* **Response (200 OK)**: Paginated JSON array of student profiles.

### 2. Get Student by ID
* **Endpoint**: `GET /api/v1/students/{id}`
* **Authorization**: `ADMIN`, `FACULTY`, `STUDENT`
* **Object-Level Security**:
  * `ADMIN` and `FACULTY` can retrieve any student record.
  * `STUDENT` callers can only retrieve their own record (where `student.userId == authenticatedUserId`). Any attempt to access another student's record returns `403 Forbidden`.

### 3. Create Student Profile
* **Endpoint**: `POST /api/v1/students`
* **Authorization**: `ADMIN` only.
* **Request Payload:**
```json
{
  "userId": 101,
  "studentNumber": "STU2026001",
  "firstName": "Srijan",
  "lastName": "Bojja",
  "dateOfBirth": "2005-08-15",
  "phone": "9876543210",
  "department": "Computer Science and Engineering",
  "program": "B.Tech CSE",
  "yearOfStudy": 2,
  "section": "A"
}
```
* **Success Response (201 Created)**: Returns created `StudentResponse`.
* **Errors**:
  * `400 Bad Request`: Auth user is inactive, lacks `STUDENT` role, or payload failed validation.
  * `409 Conflict`: `userId` or `studentNumber` already registered.

### 4. Update Student Profile
* **Endpoint**: `PUT /api/v1/students/{id}`
* **Authorization**: `ADMIN` only.
* **Request Payload:** Mutable student profile fields (`firstName`, `lastName`, `dateOfBirth`, `phone`, `department`, `program`, `yearOfStudy`, `section`).

### 5. Update Student Status
* **Endpoint**: `PATCH /api/v1/students/{id}/status`
* **Authorization**: `ADMIN` only.
* **Request Payload:**
```json
{
  "status": "ACTIVE"
}
```
* **Supported Statuses**: `ACTIVE`, `INACTIVE`, `SUSPENDED`, `GRADUATED`.

### OpenAPI / Swagger UI Documentation
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON Spec: `http://localhost:8082/v3/api-docs`

---

## 7. Build & Execution

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
java -jar target/student-service-1.0.0.jar
```

---

## 8. Docker

### Build Image
```bash
docker build -t smartcampus/student-service:1.0.0 .
```

### Run Container
```bash
docker run -d -p 8082:8082 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=smart_campus_student \
  -e DB_USERNAME=smartcampus_user \
  -e DB_PASSWORD=${DB_PASSWORD} \
  -e JWT_SECRET=${JWT_SECRET} \
  -e AUTH_SERVICE_URL=http://auth-service:8081 \
  smartcampus/student-service:1.0.0
```
