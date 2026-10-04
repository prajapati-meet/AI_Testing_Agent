# Exploration Service (`exploration-service`)

Playwright-powered web application crawler and DOM structure discovery service for **AI Test Agent**.

## Configuration
- **Port**: `8082`
- **Eureka Service Name**: `EXPLORATION-SERVICE`
- **Database**: `exploration_db` (PostgreSQL)
- **Health Endpoint**: `GET http://localhost:8082/actuator/health`

## Endpoints
| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/api/exploration/start` | Start exploring a target web application |
| `GET` | `/api/exploration/{id}` | Get exploration status (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`) |
| `GET` | `/api/exploration/{id}/pages` | Get discovered pages and element summaries |

## Build & Run
```bash
# Run unit tests
mvn clean test

# Start Exploration Service (requires PostgreSQL exploration_db)
mvn spring-boot:run
```
