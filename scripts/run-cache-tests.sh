#!/usr/bin/env bash
# Runs Minecraft-independent cache, shader inspection, and render-graph tests with JDK 17+.
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
  src/test/java/dev/opticore/client/shader/ShaderPackMetadataSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderPackInventory.java \
  src/test/java/dev/opticore/client/shader/ShaderPackInventorySelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderProgramIndex.java \
  src/test/java/dev/opticore/client/shader/ShaderProgramIndexSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderSourcePreflight.java \
  src/test/java/dev/opticore/client/shader/ShaderSourcePreflightSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderIncludeResolver.java \
  src/test/java/dev/opticore/client/shader/ShaderIncludeResolverSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderGlslInterfaceScanner.java \
  src/test/java/dev/opticore/client/shader/ShaderGlslInterfaceScannerSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderStageInterfaceValidator.java \
  src/test/java/dev/opticore/client/shader/ShaderStageInterfaceValidatorSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderProgramDescriptor.java \
  src/test/java/dev/opticore/client/shader/ShaderProgramDescriptorSelfTest.java \
  src/main/java/dev/opticore/client/shader/SceneUniformSnapshot.java \
  src/main/java/dev/opticore/client/shader/SceneEnvironmentSnapshot.java \
  src/test/java/dev/opticore/client/shader/SceneEnvironmentSnapshotSelfTest.java \
  src/test/java/dev/opticore/client/shader/SceneUniformSnapshotSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderRenderGraph.java \
  src/test/java/dev/opticore/client/shader/ShaderRenderGraphSelfTest.java \
  src/main/java/dev/opticore/client/shader/ShaderRenderGraphExecutor.java \
  src/test/java/dev/opticore/client/shader/ShaderRenderGraphExecutorSelfTest.java
java -cp "$OUT" dev.opticore.cache.SmartChunkCacheSelfTest
java -cp "$OUT" dev.opticore.client.FrameTimeMonitorSelfTest
java -cp "$OUT" dev.opticore.client.ZoomControllerSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderPackOptionStoreSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderPackArchiveValidatorSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderPackMetadataSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderPackInventorySelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderProgramIndexSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderSourcePreflightSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderIncludeResolverSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderGlslInterfaceScannerSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderStageInterfaceValidatorSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderProgramDescriptorSelfTest
java -cp "$OUT" dev.opticore.client.shader.SceneUniformSnapshotSelfTest\njava -cp "$OUT" dev.opticore.client.shader.SceneEnvironmentSnapshotSelfTest\njava -cp "$OUT" dev.opticore.client.shader.SceneEnvironmentSnapshotSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderRenderGraphSelfTest
java -cp "$OUT" dev.opticore.client.shader.ShaderRenderGraphExecutorSelfTest
