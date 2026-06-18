#!/usr/bin/env sh
# 将本目录 *.yaml 发布到 Nacos（本地 / Docker 部署共用）
set -eu

NACOS_ADDR="${NACOS_ADDR:-http://127.0.0.1:8848}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
CONFIG_DIR="${1:-$(dirname "$0")}"

echo "[nacos-import] server=${NACOS_ADDR} group=${NACOS_GROUP} dir=${CONFIG_DIR}"

for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
  if curl -sf "${NACOS_ADDR}/nacos/v1/console/health/readiness" >/dev/null 2>&1 \
    || curl -sf "${NACOS_ADDR}/nacos/" >/dev/null 2>&1; then
    break
  fi
  echo "[nacos-import] waiting for nacos..."
  sleep 3
done

for file in "${CONFIG_DIR}"/*.yaml; do
  [ -f "${file}" ] || continue
  data_id="$(basename "${file}")"
  echo "[nacos-import] publish ${data_id}"
  curl -sf -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    -F "dataId=${data_id}" \
    -F "group=${NACOS_GROUP}" \
    -F "type=yaml" \
    -F "content=<${file}"
  echo ""
done

echo "[nacos-import] done"
