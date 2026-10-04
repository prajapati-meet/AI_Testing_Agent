# Auth Service (`auth-service`)

Authentication, user registration, BCrypt password hashing, JWT management, and role control (`USER`, `ADMIN`) for **AI Test Agent**.

## Configuration
- **Port**: `8081`
- **Eureka Service Name**: `AUTH-SERVICE`
- **Database**: `auth_db` (PostgreSQL)
- **Health Endpoint**: `GET http://localhost:8081/actuator/health`

## Endpoints
| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Authenticate user credentials |
| `GET` | `/api/auth/me` | Retrieve current user profile |

## Build & Run
```bash
# Run unit tests
mvn clean test

# Start Auth Service (requires PostgreSQL auth_db)
mvn spring-boot:run
```
