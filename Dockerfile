# Everything in one image: Postgres, the Spring Boot API, and the Angular app it serves.
#
# This is deliberately not how a production system would be arranged. A database belongs outside
# the application it serves, so either can be restarted, scaled or backed up without the other.
# Here the goal is a single command that gives a working system on any machine with Docker, and for
# that one image is simpler than orchestrating three. The compromises are stated in the README:
# the data lives in a volume, and only one replica of this image can ever run.

# ---------------------------------------------------------------------------
# 1. Build the Angular application
# ---------------------------------------------------------------------------
FROM node:22-alpine AS frontend-build
WORKDIR /build

# The Angular build defaults to one worker per core and a large heap, which is more than a modest
# Docker VM has to give. Capping both keeps the build inside the container reliable.
ENV NG_BUILD_MAX_WORKERS=1 \
    NODE_OPTIONS=--max-old-space-size=1024

# Dependencies first, so a change to the application code does not reinstall them.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund --ignore-scripts=false

COPY frontend/ ./
RUN npm run build

# ---------------------------------------------------------------------------
# 2. Build the Spring Boot application, with the Angular build inside it
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /build

COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src
# Spring serves whatever is on the classpath under /static, so the compiled UI goes there and the
# whole system ships as one jar.
COPY --from=frontend-build /build/dist/frontend/browser/ ./src/main/resources/static/
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# 3. The image that actually runs: Postgres plus a JRE
# ---------------------------------------------------------------------------
FROM postgres:16-alpine

RUN apk add --no-cache openjdk21-jre-headless

# Defaults for a database that is only reachable from inside this container: Postgres is started
# with listen_addresses=localhost, and only the application port is published. They are
# overridable, and a deployment using a database outside the container should override DATABASE_*
# with real credentials supplied by the platform rather than anything baked into an image.
ENV PGDATA=/var/lib/postgresql/data \
    POSTGRES_DB=salary \
    POSTGRES_USER=salary \
    POSTGRES_PASSWORD=salary \
    DATABASE_URL=jdbc:postgresql://localhost:5432/salary \
    DATABASE_USER=salary \
    DATABASE_PASSWORD=salary \
    JAVA_OPTS="-XX:MaxRAMPercentage=60"

COPY --from=backend-build /build/target/*.jar /app/salary-management.jar
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE 8080

# The database is written here. Mount a volume on it to keep data between runs.
VOLUME ["/var/lib/postgresql/data"]

# PORT is honoured rather than assumed, since a host may well inject its own.
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=5 \
    CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
