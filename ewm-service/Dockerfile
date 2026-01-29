FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Копируем всё что нужно для сборки
COPY pom.xml .
COPY checkstyle.xml .

# Копируем stats модуль (чтобы Maven не ругался)
COPY stats/pom.xml ./stats/
COPY stats/stats-dto/pom.xml ./stats/stats-dto/
COPY stats/stats-client/pom.xml ./stats/stats-client/
COPY stats/stats-server/pom.xml ./stats/stats-server/

# Копируем исходники stats (можно без них, если только pom.xml)
COPY stats/stats-dto/src ./stats/stats-dto/src 2>/dev/null || echo "stats-dto/src not found"
COPY stats/stats-client/src ./stats/stats-client/src 2>/dev/null || echo "stats-client/src not found"
COPY stats/stats-server/src ./stats/stats-server/src 2>/dev/null || echo "stats-server/src not found"

# Копируем ewm-service
COPY ewm-service/pom.xml ./ewm-service/
COPY ewm-service/src ./ewm-service/src/

# Собираем ewm-service с зависимостями
RUN mvn clean package -DskipTests -pl ewm-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ewm-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]