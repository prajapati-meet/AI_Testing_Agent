# Project Roadmap

## Phase 1: Base Project & Microservice Foundation (Current Stage)
- [x] Establish repository structure for two-developer Git collaboration.
- [x] Set up Netflix Eureka `service-registry` (`8761`) and Spring Cloud `api-gateway` (`8080`).
- [x] Create microservice skeletons for `auth-service`, `exploration-service`, `test-service`, `execution-service`, and `bug-service`.
- [x] Configure dedicated PostgreSQL datasource schemas (`auth_db`, `exploration_db`, `test_db`, `execution_db`, `bug_db`).
- [x] Create `frontend` React dashboard skeleton (`5173`) and `demo-target-app` React e-commerce skeleton (`3000`).
- [x] Configure Actuator `/actuator/health` endpoints and initial JUnit 5 tests.

---

## Phase 2: Authentication & Gateway Security
- [ ] Complete BCrypt user registration and login verification in `auth-service`.
- [ ] Implement JWT issuance (`USER` and `ADMIN` roles) without sensitive data in claims.
- [ ] Add JWT validation filter in `api-gateway` for protected routes.
- [ ] Connect `frontend` Login and Register pages to `/api/auth/login` and `/api/auth/register`.

---

## Phase 3: Playwright Web Exploration Engine (`OBSERVE`)
- [ ] Implement headless Playwright browser navigation in `exploration-service`.
- [ ] Discover links, buttons, forms, input fields, and visible text on the target web application.
- [ ] Persist structured page representations in `exploration_db`.

---

## Phase 4: Agentic Test Generation (`REASON` & `PLAN`)
- [ ] Design structured JSON test case and step schema (`navigate`, `click`, `fill`, `wait`, `assert`, `screenshot`).
- [ ] Integrate LangGraph and LLM with `test-service` to convert discovered page models into structured test plans.
- [ ] Validate generated JSON plans before persisting to `test_db`.

---

## Phase 5: Deterministic Playwright Execution Engine (`ACT` & `OBSERVE RESULT`)
- [ ] Implement step-by-step Playwright test runner in `execution-service`.
- [ ] Capture pass/fail status, error logs, timeouts, and failure screenshots.
- [ ] Implement intentional failure toggle (`BUG_MODE=true`) scenarios in `demo-target-app`.

---

## Phase 6: Intelligent Bug Analysis (`ANALYZE`) & Dashboard Integration
- [ ] Integrate LangGraph/LLM in `bug-service` to analyze failed execution traces and generate structured bug reports.
- [ ] Build interactive views in `frontend` to trigger explorations, run generated tests, and inspect AI-generated bug reports.
