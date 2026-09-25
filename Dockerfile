# Estágio 1: Build da aplicação com Maven (versão fixada)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache de camadas: copia só o pom.xml primeiro
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte e gera o artefato final
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Imagem base leve de execução com versão fixada
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV TZ=America/Sao_Paulo

# Regra obrigatória: Criar grupo e utilizador não privilegiado (não-root)
RUN addgroup -S spring && adduser -S spring -G spring

# Copia o artefato já definindo a permissão para o utilizador não-root
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Altera para o utilizador não-root antes de rodar
USER spring:spring

EXPOSE 8080

# Healthcheck recomendado na Secção 3 (verifica se a porta 8080 responde)
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 0

ENTRYPOINT ["java", "-jar", "app.jar"]