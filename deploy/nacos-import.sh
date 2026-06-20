#!/usr/bin/env bash
# 重新将 nacos-config/*.yaml 发布到生产 Nacos（改配置后执行）
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.prod.yaml"
CONFIG_DIR="${ROOT_DIR}/services/cardiology-cloud/nacos-config"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 ${ENV_FILE}"
  exit 1
fi

cd "${ROOT_DIR}"

run_via_compose() {
  docker compose -f "${COMPOSE_FILE}" --env-file deploy/.env run --rm nacos-init
}

run_via_alpine_on_network() {
  local net="${COMPOSE_NETWORK:-cardiology-prod-net}"
  echo "[nacos-import] fallback: alpine on network ${net}"
  docker run --rm \
    --network "${net}" \
    -v "${CONFIG_DIR}:/nacos-config:ro" \
    -e NACOS_ADDR=http://nacos:8848 \
    docker.m.daocloud.io/library/alpine:3.20 \
    sh -c 'apk add --no-cache curl >/dev/null && /bin/sh /nacos-config/import.sh /nacos-config'
}

if run_via_compose; then
  exit 0
fi

echo "[nacos-import] compose run failed, trying alpine fallback..."
run_via_alpine_on_network
