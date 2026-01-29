FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Копируем только необходимые части проекта
COPY pom.xml .
COPY checkstyle.xml .
COPY stats/pom.xml ./stats/
COPY stats/stats-server/pom.xml ./stats/stats-server/
COPY stats/stats-dto/pom.xml ./stats/stats-dto/
COPY stats/stats-client/pom.xml ./stats/stats-client/

RUN mvn dependency:go-offline -B

COPY stats/stats-dto/src ./stats/stats-dto/src
COPY stats/stats-client/src ./stats/stats-client/src
COPY stats/stats-server/src ./stats/stats-server/src

RUN mvn clean package -DskipTests -pl stats/stats-server -am

# Финальный образ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/stats/stats-server/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=ci"]