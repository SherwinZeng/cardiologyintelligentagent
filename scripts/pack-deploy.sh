#!/usr/bin/env bash
# 本机打包部署包（GitHub 拉不动时用 scp 上传）
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-${ROOT_DIR}/cardiology-deploy.tgz}"
cd "${ROOT_DIR}"

tar czf "${OUT}" \
  --exclude='.git' \
  --exclude='.cursor' \
  --exclude='.tmp-shot' \
  --exclude='cardiology-deploy.tgz' \
  --exclude='**/node_modules' \
  --exclude='**/target' \
  --exclude='**/.venv' \
  --exclude='**/venv' \
  --exclude='**/__pycache__' \
  --exclude='**/.pytest_cache' \
  --exclude='deploy/.env' \
  .

echo "[pack-deploy] ${OUT} ($(du -h "${OUT}" | awk '{print $1}'))"
echo "上传: scp ${OUT} root@<服务器IP>:~/"
echo "服务器: mkdir -p ~/CardiologyIntelligentAgent && cd ~/CardiologyIntelligentAgent && tar xzf ~/${OUT##*/} && ./deploy/deploy.sh up -d --build"
