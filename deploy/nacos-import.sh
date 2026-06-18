#!/usr/bin/env bash
# 重新将 nacos-config/*.yaml 发布到生产 Nacos（改配置后执行）
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 ${ENV_FILE}"
  exit 1
fi

cd "${ROOT_DIR}"
docker compose -f docker-compose.prod.yaml --env-file deploy/.env run --rm nacos-init
