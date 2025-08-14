# --- Stage 1: Build ---
FROM eclipse-temurin:22-jdk-ubi9-minimal AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

# --- Stage 2: Run ---
FROM eclipse-temurin:22-jdk-ubi9-minimal

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Expõe a porta que a aplicação vai usar (Railway define em $PORT)
EXPOSE 8080

# Ativa logs detalhados no Spring Boot
ENV JAVA_OPTS="-Dlogging.level.root=DEBUG -Dlogging.level.org.springframework=DEBUG -Dspring.main.banner-mode=off"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
