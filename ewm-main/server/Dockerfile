FROM eclipse-temurin:21-jdk

# Метаданные
LABEL maintainer="your-email@example.com"
LABEL description="EWM Main Server"

# Аргумент для версии JAR (по умолчанию SNAPSHOT)
ARG JAR_FILE=ewm-main-server-0.0.1-SNAPSHOT.jar

# Рабочая директория
WORKDIR /app

# Копируем JAR
COPY target/${JAR_FILE} app.jar

# ENV для профиля (опционально, если docker-compose не переопределяет)
ENV SPRING_PROFILES_ACTIVE=docker

# Экспонируем порт
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]