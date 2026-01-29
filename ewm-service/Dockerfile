# Этап сборки
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Копируем всё что нужно
COPY pom.xml .
COPY checkstyle.xml .
COPY ewm-service/pom.xml ./ewm-service/
COPY ewm-service/src ./ewm-service/src/

# Собираем ewm-service
RUN mvn clean package -DskipTests -pl ewm-service -am

# Финальный образ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ewm-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]