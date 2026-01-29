FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Копируем POM файлы
COPY pom.xml .

# Копируем checkstyle.xml БЕЗ ПРОБЛЕМ - используем ADD вместо COPY
ADD checkstyle.xml .

# Копируем stats модули
COPY stats/pom.xml ./stats/
COPY stats/stats-dto/pom.xml ./stats/stats-dto/
COPY stats/stats-client/pom.xml ./stats/stats-client/
COPY stats/stats-server/pom.xml ./stats/stats-server/

# Копируем ewm-service
COPY ewm-service/pom.xml ./ewm-service/
COPY ewm-service/src ./ewm-service/src/

# Собираем
RUN mvn clean package -DskipTests -pl ewm-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/ewm-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]