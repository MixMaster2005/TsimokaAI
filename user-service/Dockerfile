# syntax=docker/dockerfile:1
# Build context attendu : la racine du repo (voir docker-compose.yml)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY user-service/pom.xml user-service/pom.xml
RUN mvn -q -pl common,user-service -am dependency:go-offline || true

COPY common common
COPY user-service user-service
RUN mvn -q -pl common,user-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
