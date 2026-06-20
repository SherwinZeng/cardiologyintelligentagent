#!/usr/bin/env bash
# 本地中间件：启动 Docker + 导入 Nacos 配置
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
docker compose up -d
"${ROOT_DIR}/deploy/nacos-import.sh"
echo "[local-docker] middleware ready (mysql/redis/postgres/nacos/rabbitmq/sentinel-dashboard)"
