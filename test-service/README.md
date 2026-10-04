# Test / AI Service (`test-service`)

AI-driven test case generation service for **AI Test Agent**. Coordinates with the planned LangGraph + LLM agent to produce structured JSON test cases from discovered page metadata.

## Configuration
- **Port**: `8083`
- **Eureka Service Name**: `TEST-SERVICE`
- **Database**: `test_db` (PostgreSQL)
- **Health Endpoint**: `GET http://localhost:8083/actuator/health`

## Endpoints
| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/api/tests/generate` | Generate structured test cases |
| `GET` | `/api/tests` | List all test cases |
| `GET` | `/api/tests/{id}` | Retrieve a test case by ID |

## Build & Run
```bash
# Run unit tests
mvn clean test

# Start Test Service (requires PostgreSQL test_db)
mvn spring-boot:run
```
