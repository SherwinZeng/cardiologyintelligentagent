#!/usr/bin/env sh
# 将本目录 *.yaml 发布到 Nacos（本地 / Docker 部署共用）
set -u

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

publish_config() {
  data_id="$1"
  group="$2"
  type="$3"
  file="$4"
  echo "[nacos-import] publish ${data_id}"
  response="$(curl -sS -w '\n%{http_code}' -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    -F "dataId=${data_id}" \
    -F "group=${group}" \
    -F "type=${type}" \
    -F "content=@${file}")"
  body="${response%$'\n'*}"
  code="${response##*$'\n'}"
  if [ "${code}" != "200" ]; then
    echo "[nacos-import] FAILED ${data_id} http=${code} body=${body}" >&2
    return 1
  fi
  echo "[nacos-import] ok ${data_id} -> ${body}"
  return 0
}

failed=0

for file in "${CONFIG_DIR}"/*.yaml; do
  [ -f "${file}" ] || continue
  data_id="$(basename "${file}")"
  if ! publish_config "${data_id}" "${NACOS_GROUP}" "yaml" "${file}"; then
    failed=1
  fi
done

for file in "${CONFIG_DIR}"/sentinel-*.json; do
  [ -f "${file}" ] || continue
  data_id="$(basename "${file}")"
  if ! publish_config "${data_id}" "SENTINEL_GROUP" "json" "${file}"; then
    failed=1
  fi
done

if [ "${failed}" -ne 0 ]; then
  echo "[nacos-import] finished with errors" >&2
  exit 1
fi

echo "[nacos-import] done"
