#!/bin/bash
# Распаковать архив в каталог на сервере.
# Использование: ./deploy/scripts/unarchive.sh [каталог] [путь_к_архиву]
# По умолчанию: ~/logistic и deploy-archive.tar.gz в корне репозитория.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TARGET="${1:-$HOME/logistic}"
ARCHIVE="${2:-$ROOT/deploy-archive.tar.gz}"

mkdir -p "$TARGET"
tar -xzf "$ARCHIVE" -C "$TARGET"
echo "Extracted $ARCHIVE to $TARGET"
