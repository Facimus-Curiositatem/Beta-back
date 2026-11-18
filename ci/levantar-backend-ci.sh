#!/usr/bin/env bash
# Se ejecuta EN LA VM DEL BACKEND (beta-back) por SSH, durante el CI de un pull request.
# Levanta un backend temporal con la imagen del PR, conectado a la BD de pruebas
# proyecto_ci (nunca a la de la app publicada). Uso:
#
#   ssh servidor 'bash -s' < ci/levantar-backend-ci.sh <imagen>
#
# Necesita el archivo ~/ci/ci.env (lo escribe el workflow) con DB_PASSWORD y JWT_SECRET.
# Imprime en la ultima linea: PUERTO=<puerto elegido>
set -euo pipefail

IMAGEN="${1:?Falta la imagen a probar}"
CONTENEDOR="beta-back-ci"
DB_HOST="${DB_HOST:-10.43.99.53}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_CI="proyecto_ci"
# Rango de puertos permitido en la politica de Tailscale para tag:ci
PUERTO_MIN=8081
PUERTO_MAX=8090
ENV_FILE="$HOME/ci/ci.env"

[ -f "$ENV_FILE" ] || { echo "No existe $ENV_FILE"; exit 1; }
# shellcheck disable=SC1090
source "$ENV_FILE"

# psql desde un contenedor: no hace falta instalar el cliente de PostgreSQL en la VM
psql_ci() {
  docker run --rm --network host -e PGPASSWORD="$DB_PASSWORD" postgres:18-alpine \
    psql -v ON_ERROR_STOP=1 -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$@"
}

# 1. Si quedo un backend de pruebas de una ejecucion anterior (cancelada), se quita
if docker ps -a --format '{{.Names}}' | grep -qx "$CONTENEDOR"; then
  echo "Quitando contenedor $CONTENEDOR de una ejecucion anterior"
  docker rm -f "$CONTENEDOR" > /dev/null
fi

# 2. Elegir el primer puerto libre del rango (ni procesos ni contenedores escuchando)
PUERTO=""
for p in $(seq "$PUERTO_MIN" "$PUERTO_MAX"); do
  if ! ss -ltnH "( sport = :$p )" | grep -q .; then
    PUERTO=$p
    break
  fi
done
if [ -z "$PUERTO" ]; then
  echo "No hay puertos libres entre $PUERTO_MIN y $PUERTO_MAX en $(hostname):"
  ss -ltn
  exit 1
fi
echo "Puerto libre elegido: $PUERTO"

# 3. BD de pruebas: se deja vacia. proyecto_ci se crea una sola vez a mano en la VM de la BD
#    (CREATE DATABASE proyecto_ci OWNER admin) y pg_hba.conf solo permite a admin entrar a ella.
echo "Vaciando $DB_CI"
psql_ci -d "$DB_CI" -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"

# 4. Descargar y arrancar la imagen del PR.
#    ddl-auto=create: Hibernate crea todas las tablas al arrancar (BD limpia en cada ejecucion).
docker pull -q "$IMAGEN"
docker run -d --name "$CONTENEDOR" --restart no \
  -p "$PUERTO:8080" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST="$DB_HOST" -e DB_PORT="$DB_PORT" -e DB_NAME="$DB_CI" \
  -e DB_USER="$DB_USER" -e DB_PASSWORD="$DB_PASSWORD" \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=create \
  -e JWT_SECRET="$JWT_SECRET" \
  -e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60" \
  --memory 768m \
  "$IMAGEN" > /dev/null

echo "Backend de pruebas arrancando en $(hostname):$PUERTO"
echo "PUERTO=$PUERTO"
