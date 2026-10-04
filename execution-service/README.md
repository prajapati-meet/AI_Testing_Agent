# Execution Service (`execution-service`)

Deterministic Playwright browser test execution runner for **AI Test Agent**.

## Configuration
- **Port**: `8084`
- **Eureka Service Name**: `EXECUTION-SERVICE`
- **Database**: `execution_db` (PostgreSQL)
- **Health Endpoint**: `GET http://localhost:8084/actuator/health`

## Planned Playwright Actions (MVP)
- `navigate`
- `click`
- `fill`
- `wait`
- `assert`
- `screenshot`

## Execution States
`PENDING`, `RUNNING`, `PASSED`, `FAILED`, `TIMEOUT`, `ERROR`

## Endpoints
| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/api/executions` | Start a new test execution |
| `GET` | `/api/executions` | List all test executions |
| `GET` | `/api/executions/{id}` | Get execution details by ID |

## Build & Run
```bash
# Run unit tests
mvn clean test

# Start Execution Service (requires PostgreSQL execution_db)
mvn spring-boot:run
```
