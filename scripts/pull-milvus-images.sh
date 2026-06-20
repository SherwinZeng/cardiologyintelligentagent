#!/usr/bin/env bash
# 逐个拉 Milvus 镜像（网络慢就重跑；先去掉 Docker 里的阿里云 registry-mirrors）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

pull() {
  local image=$1
  local n=1
  until docker pull "$image"; do
    echo ">>> 失败，10 秒后重试 (${n}/10): $image"
    n=$((n + 1))
    [[ $n -le 10 ]] || { echo "放弃: $image"; exit 1; }
    sleep 10
  done
}

echo "=== 1/3 milvusdb/etcd ==="
pull milvusdb/etcd:3.5.18-r1

echo "=== 2/3 minio ==="
pull minio/minio:RELEASE.2024-05-28T17-19-04Z

echo "=== 3/3 milvus（最大，耐心等）==="
pull milvusdb/milvus:v2.5.4

echo "=== 启动 ==="
docker compose up -d milvus-etcd milvus-minio milvus

for i in $(seq 1 24); do
  if docker exec cardiology-milvus curl -sf http://127.0.0.1:9091/healthz >/dev/null 2>&1; then
    echo "Milvus OK"
    exit 0
  fi
  sleep 5
done
echo "未就绪: docker compose logs milvus"
exit 1
