# Production image (Render, Fly.io, Railway, Koyeb, Cloud Run...).
# Local development with hot reload uses Dockerfile.dev (see docker-compose.yml).

# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
RUN mvn -B -q dependency:go-offline -DskipTests
COPY src src
RUN mvn -B -q package -DskipTests \
 && cp target/psyke-*.jar /app/app.jar

# ---- run ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S psyke && adduser -S psyke -G psyke
WORKDIR /app
COPY --from=build --chown=psyke:psyke /app/app.jar app.jar
USER psyke

# Container-aware memory (fits 512 MB instances), fail fast on OOM so the platform restarts us.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-8080}/actuator/health" | grep -q UP || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
