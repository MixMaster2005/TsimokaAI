# syntax=docker/dockerfile:1
# Build context attendu : la RACINE du repo (voir docker-compose.yml: build.context: .)
# car ce module dépend du module "common" du même réacteur Maven.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Couche de cache : on copie d'abord les POM seuls pour ne retélécharger les
# dépendances que si un pom.xml change, pas à chaque modif de code source.
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY ai-common/pom.xml ai-common/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY space-service/pom.xml space-service/pom.xml
COPY ingestion-service/pom.xml ingestion-service/pom.xml
COPY chat-service/pom.xml chat-service/pom.xml
COPY fiche-service/pom.xml fiche-service/pom.xml
COPY analytics-service/pom.xml analytics-service/pom.xml
COPY gamification-service/pom.xml gamification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked mvn -q -pl common,api-gateway -am dependency:go-offline || true

COPY common common
COPY api-gateway api-gateway
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked mvn -q -pl common,api-gateway -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
