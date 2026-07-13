# syntax=docker/dockerfile:1

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Poms first, so the dependency layer stays cached across source edits.
COPY pom.xml .
COPY domain/model/pom.xml domain/model/
COPY domain/usecase/pom.xml domain/usecase/
COPY infrastructure/driven-adapters/r2dbc-postgresql/pom.xml infrastructure/driven-adapters/r2dbc-postgresql/
COPY infrastructure/entry-points/reactive-web/pom.xml infrastructure/entry-points/reactive-web/
COPY applications/app-service/pom.xml applications/app-service/
COPY coverage-report/pom.xml coverage-report/
RUN mvn -B -q dependency:go-offline -DskipTests

COPY domain domain
COPY infrastructure infrastructure
COPY applications applications

# Tests run in CI, where a failure blocks the merge. Running them here would need a Docker daemon
# inside the build for Testcontainers.
RUN mvn -B -q clean package -DskipTests -pl applications/app-service -am

# ---------- runtime ----------
FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=build /build/applications/app-service/target/franchise-api.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx: the heap follows the memory the task is given, so
# resizing the Fargate task does not mean rebuilding the image.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=aws

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
