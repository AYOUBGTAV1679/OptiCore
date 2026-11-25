#!/usr/bin/env python3
"""Static safety checks for OptiCore's client-only Mixin registration."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
config_path = root / "src/main/resources/opticore.mixins.json"
config = json.loads(config_path.read_text(encoding="utf-8"))

assert config.get("required") is False, "Mixin config must remain non-global/optional"
assert config.get("package") == "dev.opticore.mixin", "Unexpected Mixin package"
assert config.get("mixins") == [], "No common/server Mixin should be registered"
client_mixins = config.get("client")
expected_client_mixins = {
    "LevelRendererDiagnosticsMixin",
    "ChunkCompileResultsAccessor",
    "RebuildTaskRegionAccessor",
    "ChunkRebuildCaptureMixin",
    "RenderChunkRegionAccessor",
}
assert isinstance(client_mixins, list), "Client Mixin list is missing"
assert len(client_mixins) == len(set(client_mixins)), "Duplicate client Mixin registration"
assert expected_client_mixins.issubset(set(client_mixins)), (
    "Required diagnostics and chunk-capture Mixins must be registered"
)
assert "RebuildTaskOuterAccessor" not in client_mixins, (
    "Do not depend on a compiler-generated enclosing-instance field"
)

mixin_path = root / "src/main/java/dev/opticore/mixin/LevelRendererDiagnosticsMixin.java"
source = mixin_path.read_text(encoding="utf-8")
expected_descriptor = (
    "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;"
    "Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V"
)
assert expected_descriptor in source, "Renderer Mixin descriptor changed"
assert 'at = @At("HEAD")' in source, "Diagnostic injection point changed"
assert "require = 1" in source, "Injection must fail loudly if the target is missing"
assert "OPTICORE$CHUNK_LAYER_ENTRIES.incrementAndGet()" in source, (
    "Diagnostic counter missing"
)
assert "renderChunkLayer" in source and "VertexBuffer" not in source, (
    "Diagnostic hook must not start manipulating GPU buffers"
)

capture_path = root / "src/main/java/dev/opticore/mixin/ChunkRebuildCaptureMixin.java"
capture_source = capture_path.read_text(encoding="utf-8")
compile_descriptor = (
    "compile(FFFLnet/minecraft/client/renderer/ChunkBufferBuilderPack;)"
    "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask$CompileResults;"
)
assert compile_descriptor in capture_source, "Chunk compile capture descriptor changed"
assert 'at = @At("RETURN")' in capture_source, "Chunk capture must run after compile returns"
assert "ChunkMeshCapture.observeCompilation" in capture_source, "Chunk capture bridge missing"
assert "opticore$readChunkGrid(region)" in capture_source and "target.getOrigin()" in capture_source, (
    "Capture must resolve the exact target RenderChunk origin, not region offsets"
)
assert "origin.getY() >> 4" in capture_source, (
    "Capture must preserve the target vertical section coordinate"
)
accessor_path = root / "src/main/java/dev/opticore/mixin/RenderChunkRegionAccessor.java"
accessor_source = accessor_path.read_text(encoding="utf-8")
for mapped_field in ("level", "centerX", "centerZ"):
    assert f'@Accessor("{mapped_field}")' in accessor_source, (
        f"RenderChunkRegion accessor must target mapped field {mapped_field}"
    )

assert "opticore$getCenterX()" in capture_source and "opticore$getCenterZ()" in capture_source, (
    "Capture must validate the selected RenderChunk against the region center coordinates"
)
assert "chunkX != regionAccessor.opticore$getCenterX()" in capture_source and (
    "chunkZ != regionAccessor.opticore$getCenterZ()" in capture_source
), "Capture must skip uncertain center-cell attribution"

print("Mixin config checks: OK (client-only registration, diagnostics, chunk compile capture)")
