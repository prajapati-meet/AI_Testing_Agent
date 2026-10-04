# Service Registry (`service-registry`)

Netflix Eureka Server for **AI Test Agent** microservice registration and discovery.

## Configuration
- **Port**: `8761`
- **Eureka Dashboard**: `http://localhost:8761`
- **Health Endpoint**: `GET http://localhost:8761/actuator/health`

## Responsibilities
- Service registration for all backend microservices (`API-GATEWAY`, `AUTH-SERVICE`, `EXPLORATION-SERVICE`, `TEST-SERVICE`, `EXECUTION-SERVICE`, `BUG-SERVICE`).
- Service discovery lookup for load-balanced routing.

## Build & Run
```bash
# Run unit tests
mvn clean test

# Start the Eureka server
mvn spring-boot:run
```
