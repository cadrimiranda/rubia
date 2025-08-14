# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Run ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Não precisa fixar a porta, mas pode expor a padrão para desenvolvimento local
EXPOSE 8080

# Passa a porta do Railway no runtime
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
