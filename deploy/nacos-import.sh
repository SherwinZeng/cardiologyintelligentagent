#!/usr/bin/env bash
# 将 nacos-config 发布到 Nacos（本地中间件 / 生产 Compose 通用）
# 不依赖 curlimages/curl，统一用 daocloud alpine + import.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_DIR="${ROOT_DIR}/services/cardiology-cloud/nacos-config"
ALPINE_IMAGE="${NACOS_IMPORT_ALPINE_IMAGE:-docker.m.daocloud.io/library/alpine:3.20}"
COMPOSE_NETWORK="${COMPOSE_NETWORK:-}"

cd "${ROOT_DIR}"

detect_compose() {
  if [[ -f "${ROOT_DIR}/deploy/.env" ]]; then
    COMPOSE_FILE="${ROOT_DIR}/docker-compose.prod.yaml"
    COMPOSE_ENV=(--env-file "${ROOT_DIR}/deploy/.env")
    COMPOSE_PROJECT="${COMPOSE_PROJECT:-cardiology-prod}"
  else
    COMPOSE_FILE="${ROOT_DIR}/docker-compose.yaml"
    COMPOSE_ENV=()
    COMPOSE_PROJECT="${COMPOSE_PROJECT:-cardiology}"
  fi
}

ensure_nacos_running() {
  detect_compose
  if docker ps --format '{{.Names}}' | grep -qx 'cardiology-nacos'; then
    return 0
  fi
  echo "[nacos-import] starting nacos via ${COMPOSE_FILE}..."
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "${COMPOSE_ENV[@]}" up -d nacos
}

detect_network() {
  if [[ -n "${COMPOSE_NETWORK}" ]]; then
    return 0
  fi
  if docker inspect cardiology-nacos >/dev/null 2>&1; then
    COMPOSE_NETWORK="$(docker inspect cardiology-nacos \
      --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}' | awk '{print $1}')"
  fi
  if [[ -z "${COMPOSE_NETWORK}" ]]; then
    COMPOSE_NETWORK="$(docker network ls --format '{{.Name}}' | grep -E 'cardiology(-prod)?-net' | head -1 || true)"
  fi
  if [[ -z "${COMPOSE_NETWORK}" ]]; then
    echo "[nacos-import] 找不到 Docker 网络，请先: docker compose up -d nacos" >&2
    exit 1
  fi
  echo "[nacos-import] network=${COMPOSE_NETWORK}"
}

run_import() {
  docker run --rm \
    --network "${COMPOSE_NETWORK}" \
    -v "${CONFIG_DIR}:/nacos-config:ro" \
    -e NACOS_ADDR=http://nacos:8848 \
    "${ALPINE_IMAGE}" \
    sh -c 'apk add --no-cache curl >/dev/null && /bin/sh /nacos-config/import.sh /nacos-config'
}

ensure_nacos_running
detect_network
run_import
