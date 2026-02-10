# Multi-stage build для Main Service
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Копируем pom.xml и исходники
COPY pom.xml .
COPY src ./src

# Собираем проект
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем собранный JAR из builder stage
COPY --from=builder /build/target/*.jar app.jar

# ENV для профиля
ENV SPRING_PROFILES_ACTIVE=docker

# Экспонируем порт
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]
