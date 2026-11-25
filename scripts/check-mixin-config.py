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
assert config.get("client") == ["LevelRendererDiagnosticsMixin"], (
    "Expected only the client renderer diagnostics Mixin"
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

print("Mixin config checks: OK (client-only registration, descriptor, required target)")
