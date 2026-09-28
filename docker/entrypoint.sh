#!/bin/sh
# Starts Postgres, waits for it to actually accept connections, then starts the application.
#
# Two processes in one container needs a little care: if Postgres dies the application is useless,
# so the container stops rather than sitting there serving errors.
set -e

export PGDATA="${PGDATA:-/var/lib/postgresql/data}"

echo "[entrypoint] starting postgres"
# The official image's own entrypoint handles first-run initialisation, creating the database and
# the user from POSTGRES_*. Running it in the background leaves this script free to start the app.
docker-entrypoint.sh postgres &
POSTGRES_PID=$!

echo "[entrypoint] waiting for postgres to accept connections"
until pg_isready -h localhost -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; do
    if ! kill -0 "$POSTGRES_PID" 2>/dev/null; then
        echo "[entrypoint] postgres exited before it was ready" >&2
        exit 1
    fi
    sleep 1
done
echo "[entrypoint] postgres is ready"

echo "[entrypoint] starting the application"
# exec would replace this shell and orphan Postgres, so the app runs in the background and this
# script waits on whichever process stops first.
java ${JAVA_OPTS} -jar /app/salary-management.jar &
APP_PID=$!

# Stop cleanly on docker stop: shut the app down first, then the database.
trap 'echo "[entrypoint] shutting down"; kill -TERM "$APP_PID" 2>/dev/null; wait "$APP_PID" 2>/dev/null; kill -TERM "$POSTGRES_PID" 2>/dev/null; wait "$POSTGRES_PID" 2>/dev/null; exit 0' TERM INT

# If either process stops, the container has nothing useful left to do.
wait -n "$APP_PID" "$POSTGRES_PID"
EXIT_CODE=$?
echo "[entrypoint] a process exited (code ${EXIT_CODE}); stopping the container"
kill -TERM "$APP_PID" "$POSTGRES_PID" 2>/dev/null || true
exit "$EXIT_CODE"
