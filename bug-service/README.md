# Bug Analysis Service (`bug-service`)

AI-assisted bug analysis and reporting service for **AI Test Agent**. Uses LangGraph/LLM in future phases to analyze failed test executions and produce structured bug reports.

## Configuration
- **Port**: `8085`
- **Eureka Service Name**: `BUG-SERVICE`
- **Database**: `bug_db` (PostgreSQL)
- **Health Endpoint**: `GET http://localhost:8085/actuator/health`

## Severity Levels
`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

## Endpoints
| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/api/bugs/analyze/{executionId}` | Analyze a failed execution and generate a bug report |
| `GET` | `/api/bugs` | List all bug reports |
| `GET` | `/api/bugs/{id}` | Get a specific bug report by ID |

## Build & Run
```bash
# Run unit tests
mvn clean test

# Start Bug Analysis Service (requires PostgreSQL bug_db)
mvn spring-boot:run
```
