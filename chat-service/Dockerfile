# syntax=docker/dockerfile:1
# Build context attendu : la racine du repo (voir docker-compose.yml)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY chat-service/pom.xml chat-service/pom.xml
RUN mvn -q -pl common,chat-service -am dependency:go-offline || true

COPY common common
COPY chat-service chat-service
RUN mvn -q -pl common,chat-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /workspace/chat-service/target/chat-service.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
