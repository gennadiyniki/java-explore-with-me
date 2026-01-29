# Этап сборки
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# 1. Корневой pom.xml
COPY pom.xml .
COPY checkstyle.xml . 2>/dev/null || echo "checkstyle.xml не найден, пропускаем"

# 2. Родительский stats/pom.xml
COPY stats/pom.xml ./stats/

# 3. Подмодули stats
COPY stats/stats-dto/pom.xml ./stats/stats-dto/
COPY stats/stats-client/pom.xml ./stats/stats-client/
COPY stats/stats-server/pom.xml ./stats/stats-server/

# 4. Исходный код
COPY stats/stats-dto/src ./stats/stats-dto/src
COPY stats/stats-client/src ./stats/stats-client/src
COPY stats/stats-server/src ./stats/stats-server/src

# Собираем только stats-server
RUN mvn clean package -DskipTests -pl stats/stats-server -am

# Финальный образ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/stats/stats-server/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=ci"]