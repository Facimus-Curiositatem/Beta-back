#!/usr/bin/env bash
# Se ejecuta EN LA VM DEL BACKEND (beta-back) por SSH al terminar el CI del pull request,
# pasen o fallen las pruebas. Guarda los logs, quita el backend temporal, vacia la BD
# proyecto_ci y borra las credenciales temporales. Uso:
#
#   ssh servidor 'bash -s' < ci/bajar-backend-ci.sh
set -uo pipefail

CONTENEDOR="beta-back-ci"
DB_HOST="${DB_HOST:-10.43.99.53}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_CI="proyecto_ci"
ENV_FILE="$HOME/ci/ci.env"

if docker ps -a --format '{{.Names}}' | grep -qx "$CONTENEDOR"; then
  docker logs --tail 300 "$CONTENEDOR" > "$HOME/ci/backend-ci.log" 2>&1 || true
  docker rm -f "$CONTENEDOR" > /dev/null
  echo "Contenedor $CONTENEDOR eliminado"
fi

if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  docker run --rm --network host -e PGPASSWORD="$DB_PASSWORD" postgres:18-alpine \
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_CI" \
    -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;" \
    && echo "Base de datos $DB_CI vaciada"
  rm -f "$ENV_FILE"
fi

docker logout ghcr.io > /dev/null 2>&1 || true
docker image prune -f > /dev/null 2>&1 || true
