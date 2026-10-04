# REST API Overview

All client requests from the React Frontend (`http://localhost:5173`) are sent through the **API Gateway** (`http://localhost:8080`).

---

## 1. Health Endpoints

Every backend service exposes a Spring Boot Actuator health endpoint:

| Service | Direct Health URL |
| :--- | :--- |
| **Service Registry (Eureka)** | `GET http://localhost:8761/actuator/health` |
| **API Gateway** | `GET http://localhost:8080/actuator/health` |
| **Auth Service** | `GET http://localhost:8081/actuator/health` |
| **Exploration Service** | `GET http://localhost:8082/actuator/health` |
| **Test Service** | `GET http://localhost:8083/actuator/health` |
| **Execution Service** | `GET http://localhost:8084/actuator/health` |
| **Bug Analysis Service** | `GET http://localhost:8085/actuator/health` |

---

## 2. Auth Service Endpoints (`/api/auth`)

Routed via API Gateway (`http://localhost:8080/api/auth/**`) to `AUTH-SERVICE` (`8081`).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register a new user (`USER` or `ADMIN` role). |
| `POST` | `/api/auth/login` | Authenticate user credentials and return a placeholder JWT response. |
| `GET` | `/api/auth/me` | Retrieve current user profile information. |

---

## 3. Exploration Service Endpoints (`/api/exploration`)

Routed via API Gateway (`http://localhost:8080/api/exploration/**`) to `EXPLORATION-SERVICE` (`8082`).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/exploration/start` | Start a new web application exploration session for a `targetUrl`. |
| `GET` | `/api/exploration/{id}` | Retrieve status (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`) and metadata of an exploration. |
| `GET` | `/api/exploration/{id}/pages` | Retrieve discovered pages and interactive element counts for an exploration. |

---

## 4. Test / AI Service Endpoints (`/api/tests`)

Routed via API Gateway (`http://localhost:8080/api/tests/**`) to `TEST-SERVICE` (`8083`).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/tests/generate` | Generate structured test cases for a target URL / exploration ID. |
| `GET` | `/api/tests` | List all generated test cases. |
| `GET` | `/api/tests/{id}` | Get details of a specific test case by ID. |

---

## 5. Execution Service Endpoints (`/api/executions`)

Routed via API Gateway (`http://localhost:8080/api/executions/**`) to `EXECUTION-SERVICE` (`8084`).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/executions` | Trigger execution of a test case against a target URL. |
| `GET` | `/api/executions` | List all test executions and their statuses (`PENDING`, `RUNNING`, `PASSED`, `FAILED`, `TIMEOUT`, `ERROR`). |
| `GET` | `/api/executions/{id}` | Retrieve details of a specific execution by ID. |

---

## 6. Bug Analysis Service Endpoints (`/api/bugs`)

Routed via API Gateway (`http://localhost:8080/api/bugs/**`) to `BUG-SERVICE` (`8085`).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/bugs/analyze/{executionId}` | Analyze a failed execution and generate a structured bug report. |
| `GET` | `/api/bugs` | List all generated bug reports (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). |
| `GET` | `/api/bugs/{id}` | Retrieve details of a specific bug report by ID. |
