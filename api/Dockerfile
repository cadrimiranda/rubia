# --- Stage 1: Build ---
# Usa uma imagem base com JDK 21 para compilar o projeto.
# 'alpine' é uma distro leve, ideal para ambientes de build.
FROM eclipse-temurin:21-jdk-alpine AS build

# Define o diretório de trabalho dentro do container.
WORKDIR /app

# Copia o Maven wrapper e o arquivo pom.xml.
# Isso aproveita o cache do Docker para dependências que não mudaram.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixa as dependências.
# Se o pom.xml não mudar, esta etapa é ignorada em builds futuros.
RUN ./mvnw dependency:go-offline

# Copia o código fonte do projeto.
COPY src src

# Compila o projeto, executa o empacotamento e pula os testes.
# O resultado será um JAR executável em target/*.jar
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Run ---
# Usa uma imagem base com apenas o JRE 21, que é menor do que a imagem completa do JDK.
FROM eclipse-temurin:21-jre-alpine

# Define o diretório de trabalho.
WORKDIR /app

# Copia o JAR do estágio de build para o estágio de execução.
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta que o aplicativo usa.
EXPOSE 8080

# Define o comando que será executado quando o container iniciar.
ENTRYPOINT ["java", "-jar", "app.jar"]