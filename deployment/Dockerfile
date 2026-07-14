# syntax=docker/dockerfile:1

# ---------- build ----------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build

# The wrapper and the build scripts first, on their own: dependencies only change when these do,
# so this layer stays cached across every source edit.
COPY gradlew .
COPY gradle gradle
COPY settings.gradle build.gradle gradle.properties lombok.config ./
COPY domain/model/build.gradle domain/model/
COPY domain/usecase/build.gradle domain/usecase/
COPY infrastructure/driven-adapters/r2dbc-postgresql/build.gradle infrastructure/driven-adapters/r2dbc-postgresql/
COPY infrastructure/entry-points/reactive-web/build.gradle infrastructure/entry-points/reactive-web/
COPY applications/app-service/build.gradle applications/app-service/
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true

COPY domain domain
COPY infrastructure infrastructure
COPY applications applications

# Tests run in CI, where a failure blocks the merge. Running them here would need a Docker daemon
# inside the build for Testcontainers.
RUN ./gradlew --no-daemon :app-service:bootJar -x test

# ---------- runtime ----------
FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=build /build/applications/app-service/build/libs/franchise-api.jar app.jar
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
