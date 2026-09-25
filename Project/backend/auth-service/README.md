# Authentication Service (`auth-service`)

Cloud-Native Authentication and Identity Microservice for the **Smart Campus Management Platform**.

## 1. Purpose & Responsibilities

- **User Authentication & Identity Management**: Secure credential verification and token issuance.
- **Role-Based Access Control (RBAC)**: Support for `ADMIN`, `FACULTY`, and `STUDENT` roles.
- **JWT Token Generation & Validation**: Signs and verifies cryptographically secure tokens with custom claims.
- **Current User Profile Retrieval**: Exposes `/api/v1/auth/me` driven by JWT context.
- **Password Security**: Strong hashing via Spring Security `BCryptPasswordEncoder`.
- **Database Ownership**: Owns `smart_campus_auth` database with Flyway schema migration.

---

## 2. Technology Stack

- **Java 21 (LTS)**
- **Spring Boot 3.3.4**
- **Spring Data JPA & Hibernate**
- **Spring Security**
- **Flyway** (database migrations)
- **MySQL 8** (runtime database)
- **JJWT 0.12.6** (JWT token handling)
- **SpringDoc OpenAPI 2.6.0 / Swagger UI**
- **Jakarta Bean Validation**
- **JUnit 5 & Mockito** (testing)
- **Docker** (containerization)

---

## 3. Environment Configuration

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `SERVER_PORT` | Port the service listens on | `8081` |
| `DB_HOST` | MySQL host address | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Auth database name | `smart_campus_auth` |
| `DB_USERNAME` | Non-root database username | `smartcampus_user` |
| `DB_PASSWORD` | Database user password | *(Required - No default)* |
| `JWT_SECRET` | 256-bit secret key for HMAC-SHA256 | *(Required - No default)* |
| `JWT_EXPIRATION_MS` | JWT expiration time in milliseconds | `86400000` (24 hours) |
| `FRONTEND_URL` | Allowed origin for CORS | `http://localhost:5173` |

> **Security Note**: `DB_PASSWORD` and `JWT_SECRET` are mandatory environment variables and must be supplied through the environment. No default or fallback values are provided in configuration.

---

## 4. Database Schema & Flyway Migration

Flyway owns all schema migrations located in `src/main/resources/db/migration/`:

- `V1__create_auth_tables.sql`:
  - `users`: ID, username, email, password_hash, first_name, last_name, is_active, created_at, updated_at.
  - `roles`: ID, name, description (`ADMIN`, `FACULTY`, `STUDENT`).
  - `user_roles`: Foreign keys linking `users` and `roles`.

> **Note**: `spring.jpa.hibernate.ddl-auto` is set to `validate` to ensure JPA entity mappings strictly match Flyway-managed schema.

---

## 5. API Endpoints

Base path: `/api/v1/auth`

### `POST /api/v1/auth/login` (Public)
Authenticate user credentials.

**Request Payload:**
```json
{
  "username": "srijan",
  "password": "Password123"
}
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "username": "srijan",
    "email": "srijan@example.com",
    "roles": [
      "STUDENT"
    ]
  }
}
```

### `GET /api/v1/auth/me` (Protected)
Requires header `Authorization: Bearer <JWT>`.

**Success Response (200 OK):**
```json
{
  "id": 1,
  "username": "srijan",
  "email": "srijan@example.com",
  "roles": [
    "STUDENT"
  ]
}
```

### OpenAPI / Swagger UI Documentation
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON Spec: `http://localhost:8081/v3/api-docs`

---

## 6. Build & Execution

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
java -jar target/auth-service-1.0.0.jar
```

---

## 7. Docker

### Build Image
```bash
docker build -t smartcampus/auth-service:1.0.0 .
```

### Run Container
```bash
docker run -d -p 8081:8081 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=smart_campus_auth \
  -e DB_USERNAME=smartcampus_user \
  -e DB_PASSWORD=${DB_PASSWORD} \
  -e JWT_SECRET=${JWT_SECRET} \
  smartcampus/auth-service:1.0.0
```
