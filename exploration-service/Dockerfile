# ==============================================================================
# Exploration Service Dockerfile (Playwright Enabled)
# Uses Microsoft Playwright Java Jammy runtime with pre-installed browsers & system deps
# ==============================================================================

# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Playwright Java Runtime
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENV JAVA_OPTS="-Xms128m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
