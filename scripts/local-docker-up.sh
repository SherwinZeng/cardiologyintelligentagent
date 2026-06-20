#!/usr/bin/env bash
# 本地中间件：启动 Docker（无需 nacos-import）
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
docker compose up -d
echo "[local-docker] middleware ready (mysql/redis/postgres/nacos/rabbitmq/sentinel-dashboard)"
