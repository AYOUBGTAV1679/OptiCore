#!/usr/bin/env bash
# Runs Minecraft-independent cache and shader-option persistence tests with a plain JDK 17+.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=build/cache-test-classes
rm -rf "$OUT"; mkdir -p "$OUT"
javac --release 17 -Xlint:all -Werror -d "$OUT" \
  src/main/java/dev/opticore/cache/*.java src/test/java/dev/opticore/cache/*.java \
  src/main/java/dev/opticore/client/FrameTimeMonitor.java \
  src/main/java/dev/opticore/client/ZoomController.java \
  src/test/java/dev/opticore/client/FrameTimeMonitorSelfTest.java \
  src/test/java/dev/opticore/client/ZoomControllerSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderPackOptionStore.java \
  src/test/java/dev/opticore/client/shader/ShaderPackOptionStoreSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderPackArchiveValidator.java \
  src/test/java/dev/opticore/client/shader/ShaderPackArchiveValidatorSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderPropertiesParser.java \
  src/test/java/dev/opticore/client/shader/ShaderPackMetadataSelfTest.java
java -cp "$OUT" dev.opticore.cache.SmartChunkCacheSelfTest
java -cp "$OUT" dev.opticore.client.FrameTimeMonitorSelfTest
java -cp "$OUT" dev.opticore.client.ZoomControllerSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderPackOptionStoreSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderPackArchiveValidatorSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderPackMetadataSelfTest
