FROM eclipse-temurin:21-jdk
LABEL maintainer="your-email@example.com"
LABEL description="EWM Main Server"
ARG JAR_FILE=ewm-main-server-0.0.1-SNAPSHOT.jar
WORKDIR /app
COPY target/${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
