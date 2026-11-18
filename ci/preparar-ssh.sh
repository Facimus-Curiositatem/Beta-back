#!/usr/bin/env bash
# Se ejecuta en el runner de GitHub Actions, despues de conectarse a Tailscale.
# Deja listo un alias SSH ("servidor") hacia la VM indicada, usando la llave de deploy
# y la huella guardada en los secretos. Uso:
#
#   DEPLOY_SSH_KEY=... KNOWN_HOSTS=... ci/preparar-ssh.sh beta-back
#
# Exporta al resto del job: SERVIDOR_IP (IP de Tailscale de la VM).
set -euo pipefail

HOST="${1:?Indica el nombre de la VM en Tailscale, por ejemplo beta-back}"
: "${DEPLOY_SSH_KEY:?Falta el secreto DEPLOY_SSH_KEY}"
: "${KNOWN_HOSTS:?Falta el secreto con la huella del servidor}"

install -d -m 700 ~/.ssh
# tr quita retornos de carro por si el secreto se pego desde Windows
printf '%s\n' "$DEPLOY_SSH_KEY" | tr -d '\r' > ~/.ssh/deploy_key
chmod 600 ~/.ssh/deploy_key
printf '%s\n' "$KNOWN_HOSTS" | tr -d '\r' > ~/.ssh/known_hosts

# IP de la VM dentro de Tailscale (no depende del DNS del runner)
IP=""
for _ in $(seq 1 10); do
  IP=$(tailscale ip -4 "$HOST" 2>/dev/null || true)
  [ -n "$IP" ] && break
  sleep 3
done
if [ -z "$IP" ]; then
  echo "No se encontro $HOST en la red de Tailscale"
  tailscale status || true
  exit 1
fi

# HostKeyAlias: la huella del secreto esta guardada a nombre de la VM (beta-back / beta-front)
cat > ~/.ssh/config <<EOF
Host servidor
  HostName $IP
  User deploy
  IdentityFile ~/.ssh/deploy_key
  IdentitiesOnly yes
  HostKeyAlias $HOST
  StrictHostKeyChecking yes
  ServerAliveInterval 15
EOF
chmod 600 ~/.ssh/config

ssh servidor 'echo "SSH OK en $(hostname)"'
echo "SERVIDOR_IP=$IP" >> "$GITHUB_ENV"
