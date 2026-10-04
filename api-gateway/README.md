# API Gateway (`api-gateway`)

Spring Cloud Gateway serving as the single entry point for the **AI Test Agent** frontend dashboard.

## Configuration
- **Port**: `8080`
- **Eureka Service Name**: `API-GATEWAY`
- **Health Endpoint**: `GET http://localhost:8080/actuator/health`

## Configured Routes
| Path Pattern | Target Service (Eureka `lb://`) |
| :--- | :--- |
| `/api/auth/**` | `lb://AUTH-SERVICE` |
| `/api/exploration/**` | `lb://EXPLORATION-SERVICE` |
| `/api/tests/**` | `lb://TEST-SERVICE` |
| `/api/executions/**` | `lb://EXECUTION-SERVICE` |
| `/api/bugs/**` | `lb://BUG-SERVICE` |

## Build & Run
```bash
# Run unit tests
mvn clean test

# Start API Gateway
mvn spring-boot:run
```
