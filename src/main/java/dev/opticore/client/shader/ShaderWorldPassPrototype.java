package dev.opticore.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.opticore.OptiCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.Set;

/**
 * Opt-in bridge that executes the scene capture and fullscreen composite through
 * OptiCore's validated render graph at Forge's AFTER_LEVEL stage.
 */
public final class ShaderWorldPassPrototype {
    private static final ShaderRenderGraph WORLD_GRAPH = ShaderRenderGraph.builder()
            .externalResource("vanillaScene")
            .addPass("capture", Set.of("vanillaScene"),
                    Set.of("sceneColor", "sceneDepth"), Set.of(),
                    ShaderRenderGraph.DepthMode.READ_ONLY)
            .addPass("composite", Set.of("sceneColor", "sceneDepth"),
                    Set.of("compositedScene"), Set.of("capture"),
                    ShaderRenderGraph.DepthMode.READ_WRITE)
            .build();

    private static long successfulCaptures;
    private static int lastSuccessfulRenderTick = -1;
    private static String lastFailure = "";
    private static String packDiagnostic = "";
    private static FullscreenCompositePass compositePass = new FullscreenCompositePass();
    private static String loadedPackName;
    private static GlShaderProgram selectedPackProgram;

    private ShaderWorldPassPrototype() {}

    /** Called from the Forge render-stage event on the client render thread. */
    public static void onWorldRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!OptiCoreConfig.ENABLE_EXPERIMENTAL_WORLD_PASS.get()) return;

        RenderSystem.assertOnRenderThread();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        float fogStrength = OptiCoreConfig.WORLD_FOG_STRENGTH.get().floatValue();
        float warmth = OptiCoreConfig.WORLD_COLOR_WARMTH.get().floatValue();
        // A neutral pass would only copy the full-resolution scene and issue a
        // fullscreen draw. Skip it entirely to avoid needless GPU bandwidth.
        if (fogStrength == 0.0f && warmth == 0.0f) {
            lastFailure = "";
            return;
        }

        refreshSelectedPackProgram();
        RenderTarget vanillaScene = minecraft.getMainRenderTarget();
        GlRenderTarget[] capturedScene = new GlRenderTarget[1];
        boolean[] sceneCaptured = {false};
        try {
            Map<String, ShaderRenderGraphExecutor.PassHandler> handlers = Map.of(
                    "capture", pass -> {
                        capturedScene[0] = GlRenderTargetManager.acquire(
                                "world-scene-capture", vanillaScene.width, vanillaScene.height);
                        capturedScene[0].copySceneFrom(vanillaScene);
                        sceneCaptured[0] = true;
                    },
                    "composite", pass -> {
                        float[] projection = new float[16];
                        event.getProjectionMatrix().get(projection);
                        float[] inverseProjection = Matrix4Inverse.invert(projection);
                        compositePass.render(capturedScene[0], vanillaScene, warmth, fogStrength,
                                inverseProjection, selectedPackProgram);
                    });

            ShaderRenderGraphExecutor.ExecutionResult execution =
                    ShaderRenderGraphExecutor.execute(WORLD_GRAPH, handlers);
            if (!execution.success()) {
                String reason = execution.diagnostics().isEmpty()
                        ? "render graph execution failed" : String.join("; ", execution.diagnostics());
                throw new IllegalStateException(reason);
            }
            successfulCaptures++;
            lastSuccessfulRenderTick = event.getRenderTick();
            lastFailure = packDiagnostic;
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            String diagnostic = message == null || message.isBlank()
                    ? failure.getClass().getSimpleName() : message;
            if (sceneCaptured[0] && capturedScene[0] != null) {
                try {
                    capturedScene[0].copySceneTo(vanillaScene);
                    diagnostic += " (original scene restored)";
                } catch (RuntimeException restoreFailure) {
                    String restoreMessage = restoreFailure.getMessage();
                    diagnostic += " (scene restore also failed: "
                            + (restoreMessage == null || restoreMessage.isBlank()
                            ? restoreFailure.getClass().getSimpleName() : restoreMessage) + ")";
                }
            }
            lastFailure = diagnostic;
        }
    }

    /** Recreates GL objects after Minecraft reloads client resources. */
    public static void onResourcesReloaded() {
        RenderSystem.assertOnRenderThread();
        compositePass.close();
        compositePass = new FullscreenCompositePass();
        loadedPackName = null;
        selectedPackProgram = null;
    }

    /** Invalidates the selected-pack program after selection or option changes. */
    public static void invalidateShaderPackProgram() {
        RenderSystem.assertOnRenderThread();
        loadedPackName = null;
        selectedPackProgram = null;
    }

    private static void refreshSelectedPackProgram() {
        String selected = ShaderPackRepository.activePack();
        if (selected.equals(loadedPackName)) {
            if (selectedPackProgram == null || !selectedPackProgram.isClosed()) return;
            // The bounded program cache may evict this entry after other explicit
            // compile requests. Re-resolve it rather than failing every world frame.
            loadedPackName = null;
        }
        loadedPackName = selected;
        selectedPackProgram = null;
        if ("OFF".equalsIgnoreCase(selected)) {
            packDiagnostic = "";
            return;
        }

        try {
            ShaderPackRuntime.Result result = ShaderPackRuntime.compileSelectedFullscreenProgram();
            if (result.compiled()) {
                selectedPackProgram = result.program();
                packDiagnostic = "";
            } else {
                packDiagnostic = "Shader pack '" + selected
                        + "' is not usable by the fullscreen world pass: "
                        + String.join("; ", result.diagnostics());
            }
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            packDiagnostic = "Shader pack '" + selected + "' could not be prepared: "
                    + (message == null || message.isBlank()
                    ? failure.getClass().getSimpleName() : message);
        }
    }

    public static long successfulCaptures() { return successfulCaptures; }
    public static int lastSuccessfulRenderTick() { return lastSuccessfulRenderTick; }
    public static String lastFailure() { return lastFailure; }
}
