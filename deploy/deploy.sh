#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 ${ENV_FILE}，请先执行: cp deploy/.env.example deploy/.env"
  exit 1
fi

cd "${ROOT_DIR}"

docker compose -f docker-compose.prod.yaml --env-file deploy/.env "$@"
