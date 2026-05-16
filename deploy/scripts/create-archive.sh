#!/bin/bash
# Создать архив для копирования на сервер. Запускать из корня репозитория:
#   ./deploy/scripts/create-archive.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

tar -czf deploy-archive.tar.gz \
  --exclude=diploma \
  --exclude=docs \
  --exclude=utilities \
  --exclude=.git \
  --exclude=.idea \
  --exclude=.github \
  --exclude=.pytest_cache \
  --exclude=backend/build \
  --exclude=backend/.gradle \
  --exclude=frontend/logistic/node_modules \
  --exclude=frontend/logistic/dist \
  --exclude=frontend/logistic/.angular \
  --exclude=.env \
  --exclude=deploy-archive.tar.gz \
  .

echo "Created deploy-archive.tar.gz ($(du -h deploy-archive.tar.gz | cut -f1))"
