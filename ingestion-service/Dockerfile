# syntax=docker/dockerfile:1
# Build context attendu : la racine du repo (voir docker-compose.yml)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY ai-common/pom.xml ai-common/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY space-service/pom.xml space-service/pom.xml
COPY chat-service/pom.xml chat-service/pom.xml
COPY fiche-service/pom.xml fiche-service/pom.xml
COPY analytics-service/pom.xml analytics-service/pom.xml
COPY gamification-service/pom.xml gamification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked mvn -q -pl common,ingestion-service -am dependency:go-offline || true

COPY common common
COPY ingestion-service ingestion-service
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked mvn -q -pl common,ingestion-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /workspace/ingestion-service/target/ingestion-service.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
