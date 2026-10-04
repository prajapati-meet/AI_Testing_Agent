# System Architecture & Planned AI Workflow

## 1. Architectural Overview

**AI Test Agent** uses a microservice architecture built on **Spring Boot 3.x**, **Spring Cloud Gateway**, and **Netflix Eureka**, paired with a **React** frontend dashboard and **Playwright** browser automation.

### End-to-End Request Flow
```text
User
 → React Dashboard (Port 5173)
 → API Gateway (Port 8080)
 → Exploration Service (Port 8082)
 → Test/AI Service (Port 8083)
 → Execution Service (Port 8084)
 → Playwright Browser Engine
 → Target Web Application (Port 3000)
 → Execution Result
 → Bug Analysis Service (Port 8085)
 → LLM
 → Bug Report
 → React Dashboard
```

---

## 2. Microservices and Communication

1. **Service Registry (`service-registry`, Port `8761`)**
   - Runs Netflix Eureka Server.
   - All backend services (`API-GATEWAY`, `AUTH-SERVICE`, `EXPLORATION-SERVICE`, `TEST-SERVICE`, `EXECUTION-SERVICE`, `BUG-SERVICE`) register with Eureka.

2. **API Gateway (`api-gateway`, Port `8080`)**
   - Single entry point for the React Dashboard.
   - Uses Eureka load-balanced (`lb://`) service discovery to route requests:
     - `/api/auth/**` → `lb://AUTH-SERVICE`
     - `/api/exploration/**` → `lb://EXPLORATION-SERVICE`
     - `/api/tests/**` → `lb://TEST-SERVICE`
     - `/api/executions/**` → `lb://EXECUTION-SERVICE`
     - `/api/bugs/**` → `lb://BUG-SERVICE`
   - Handles CORS configuration and future centralized JWT verification.

3. **Auth Service (`auth-service`, Port `8081`, DB: `auth_db`)**
   - Manages user registration, authentication, BCrypt password hashing, JWT token generation, and role assignment (`USER`, `ADMIN`).

4. **Exploration Service (`exploration-service`, Port `8082`, DB: `exploration_db`)**
   - Uses Playwright Java to navigate to a target URL and extract structured DOM metadata (pages, links, buttons, forms, inputs, visible text).

5. **Test / AI Service (`test-service`, Port `8083`, DB: `test_db`)**
   - Receives structured page observations and coordinates with the AI Agent to generate structured JSON test cases.

6. **Execution Service (`execution-service`, Port `8084`, DB: `execution_db`)**
   - Deterministically executes structured test steps (`navigate`, `click`, `fill`, `wait`, `assert`, `screenshot`) using Playwright and records outcomes (`PENDING`, `RUNNING`, `PASSED`, `FAILED`, `TIMEOUT`, `ERROR`).

7. **Bug Analysis Service (`bug-service`, Port `8085`, DB: `bug_db`)**
   - Inspects failed executions and utilizes the LLM to synthesize structured bug reports with severity classification (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), expected vs. actual results, and suspected application areas.

---

## 3. Database Isolation Rule

Each stateful microservice owns its dedicated PostgreSQL database:
- `auth-service` → `auth_db` only
- `exploration-service` → `exploration_db` only
- `test-service` → `test_db` only
- `execution-service` → `execution_db` only
- `bug-service` → `bug_db` only

**No service may directly access another service's database.** Inter-service data exchange occurs exclusively via REST APIs.

---

## 4. Planned AI Architecture

The system follows an agentic cycle:
```text
OBSERVE → REASON → PLAN → ACT → OBSERVE RESULT → ANALYZE
```

### AI Pipeline Flow
```text
Exploration Service
       ↓
Structured Page Information
       ↓
Test/AI Service
       ↓
LangGraph Agent
       ↓
LLM
       ↓
Structured Test Cases
       ↓
Execution Service
       ↓
Playwright
       ↓
Execution Result
       ↓
Bug Analysis Service
       ↓
LangGraph / LLM
       ↓
Structured Bug Report
```

### Role of LangGraph and LangChain
- **LangGraph**: Orchestrates the stateful, multi-step agent workflow (managing state transitions between page observation analysis, test scenario planning, schema validation, and failure root-cause analysis).
- **LangChain**: Acts as the supporting LLM integration library (handling model clients, prompt templates, and structured JSON output parsers).
- Note: The AI Agent may optionally run as a dedicated Python companion service called by `test-service` and `bug-service` if Python provides cleaner LangGraph orchestration.

---

## 5. Strict Separation of Responsibilities

To guarantee safety, determinism, and reliability, the architecture enforces a strict boundary between AI reasoning and browser execution:

| Layer | Allowed Responsibilities | Prohibited Actions |
| :--- | :--- | :--- |
| **LLM** | Understands page structure, identifies user workflows, generates structured JSON test plans, analyzes failures, generates bug reports. | **Must NEVER directly execute arbitrary browser code or arbitrary system/shell code.** |
| **Spring Boot** | Authentication, API routing, data persistence, service communication, schema validation, execution control. | Must never expose raw user passwords or bypass service boundaries. |
| **Playwright** | Browser interaction (`navigate`, `click`, `fill`, `wait`, `assert`, `screenshot`) driven by validated action steps. | Must only run predefined, deterministic action primitives. |
