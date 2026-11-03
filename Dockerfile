# ---------- build ----------
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

# Baixa dependências numa camada própria para aproveitar cache
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S oficina && adduser -S oficina -G oficina
COPY --from=build /app/target/oficina-mecanica.jar app.jar
USER oficina

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
