#сборка
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# проект целиком
COPY . .

#только ewm-service
RUN mvn clean package -DskipTests -pl ewm-service -am

# Финальный образ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ewm-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]