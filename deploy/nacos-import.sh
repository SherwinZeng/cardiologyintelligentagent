#!/usr/bin/env bash
# 已废弃：配置改在各服务 application.yml，Nacos 仅服务发现，无需导入。
echo "[nacos-import] 已跳过：业务配置在 application.yml / application-docker.yml，无需发布到 Nacos。"
echo "[nacos-import] 若仍要手动导入旧 nacos-config/，可执行:"
echo "  docker run --rm --network cardiology-net -v \"\$(pwd)/services/cardiology-cloud/nacos-config:/nacos-config:ro\" \\"
echo "    -e NACOS_ADDR=http://nacos:8848 docker.m.daocloud.io/library/redis:7.2-alpine \\"
echo "    sh -c 'apk add --no-cache curl >/dev/null && /bin/sh /nacos-config/import.sh /nacos-config'"
