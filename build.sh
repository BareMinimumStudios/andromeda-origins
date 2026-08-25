#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
./gradlew --version
./gradlew build
echo "Build complete. Check build/libs/"
