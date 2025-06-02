# Build stage
FROM openjdk:24-jdk-slim AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Production stage
FROM openjdk:24-jre-slim

WORKDIR /app

# Copy the jar file from build stage
COPY --from=build /app/target/*.jar app.jar

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring
RUN chown -R spring:spring /app
USER spring

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]