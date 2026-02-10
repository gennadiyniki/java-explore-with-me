# Multi-stage build для Stats Server
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Копируем ВСЮ структуру stats модуля
COPY ../../pom.xml /build/root-pom.xml
COPY ../pom.xml /build/stats-pom.xml
COPY pom.xml /build/server-pom.xml
COPY src ./src

# Создаем структуру как в проекте
RUN mkdir -p /build/../../ && \
    cp /build/root-pom.xml /build/../../pom.xml && \
    mkdir -p /build/../ && \
    cp /build/stats-pom.xml /build/../pom.xml

# Собираем проект
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем собранный JAR из builder stage
COPY --from=builder /build/target/*.jar app.jar

# ENV для профиля
ENV SPRING_PROFILES_ACTIVE=docker

# Экспонируем порт
EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD curl -f http://localhost:9090/actuator/health || exit 1

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]