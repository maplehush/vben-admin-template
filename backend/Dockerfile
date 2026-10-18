FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home /app app

WORKDIR /app
COPY --from=builder --chown=app:app /workspace/admin-module-system/admin-system-start/target/admin-system-start-*.jar /app/app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
