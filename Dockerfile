# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copia arquivos do gradle e service-api
COPY service-api/gradlew service-api/gradlew.bat ./
COPY service-api/gradle gradle
COPY service-api/settings.gradle service-api/build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copia codigo-fonte e compila o JAR
COPY service-api/src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
