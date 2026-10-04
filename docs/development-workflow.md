# Development & Git Collaboration Workflow

## 1. Two-Developer Team Division

The repository is structured so two developers can build and test microservices independently without merge conflicts:

### Developer 1
- `service-registry/` (Eureka Server)
- `api-gateway/` (Spring Cloud Gateway)
- `auth-service/` (Spring Security, BCrypt, JWT)
- `exploration-service/` (Playwright page crawler)
- Related API and architecture documentation

### Developer 2
- `test-service/` (AI test case generation service)
- `execution-service/` (Playwright test execution runner)
- `bug-service/` (AI bug report analysis service)
- `frontend/` (React + Vite dashboard)
- `demo-target-app/` (React + Vite target e-commerce application)
- Related documentation

---

## 2. Git Branching Strategy

```text
main
 │
 ├── develop
 │
 ├── feature/auth-service
 ├── feature/exploration-service
 ├── feature/test-service
 ├── feature/execution-service
 ├── feature/bug-service
 ├── feature/frontend
 └── feature/demo-target
```

### Branch Responsibilities
- `main`: Protected production-ready branch. Never commit directly to `main`.
- `develop`: Primary integration branch for ongoing development.
- `feature/*`: Short-lived feature branches branched off `develop`.

### Step-by-Step Daily Workflow

1. **Clone and switch to `develop`:**
   ```bash
   git clone <repository-url>
   cd ai-test-agent
   git checkout develop
   git pull origin develop
   ```

2. **Create a feature branch:**
   ```bash
   git checkout -b feature/auth-service
   ```

3. **Build and run tests locally before committing:**
   ```bash
   cd auth-service
   mvn clean test
   ```

4. **Commit and push changes:**
   ```bash
   git add .
   git commit -m "feat: implement auth service base"
   git push origin feature/auth-service
   ```

5. **Open a Pull Request:**
   - Open a PR on GitHub from `feature/auth-service` into `develop`.
   - Have the teammate review and merge into `develop`.

---

## 3. Coding Standards

- **Java 21 & Spring Boot 3.x** with Maven.
- **Layered package structure** in every backend service:
  `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `config/`, `exception/`.
- **Constructor Injection**: Always use constructor injection for Spring beans (avoid `@Autowired` on fields).
- **DTO Separation**: Use DTO classes for API request/response payloads rather than exposing JPA entities directly.
- **No Lombok**: Write explicit constructors, getters, and setters (or Java records for DTOs) to keep compilation portable across IDEs.
- **Security**:
  - Never commit `.env` files, real API keys, JWT secrets, or database passwords.
  - Always hash user passwords with BCrypt.
  - Never store passwords inside JWT payloads.
