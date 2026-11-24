#!/usr/bin/env bash
# Runs Minecraft-independent cache tests with a plain JDK 17+.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=build/cache-test-classes
rm -rf "$OUT"; mkdir -p "$OUT"
javac --release 17 -Xlint:all -Werror -d "$OUT" \
  src/main/java/dev/opticore/cache/*.java src/test/java/dev/opticore/cache/*.java \
  src/main/java/dev/opticore/client/FrameTimeMonitor.java \
  src/test/java/dev/opticore/client/FrameTimeMonitorSelfTest.java
java -cp "$OUT" dev.opticore.cache.SmartChunkCacheSelfTest
java -cp "$OUT" dev.opticore.client.FrameTimeMonitorSelfTest
