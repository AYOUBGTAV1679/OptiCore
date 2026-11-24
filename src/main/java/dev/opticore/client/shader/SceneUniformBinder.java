package dev.opticore.client.shader;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Uploads the scene snapshot's common OptiFine-style uniforms to an already-bound program.
 *
 * <p>Missing uniforms are intentionally ignored by GlShaderProgram, allowing a shader pack
 * to declare only the subset it uses. This is a compatibility starter set, not a complete
 * implementation of OptiFine's uniform contract.</p>
 */
public final class SceneUniformBinder {
    private SceneUniformBinder() {}

    /** Must be called on the render thread while {@code program} is bound. */
    public static void uploadCommon(GlShaderProgram program, SceneUniformSnapshot scene) {
        uploadCommon(program, scene, null);
    }

    /** Uploads scene matrices/time and, when supplied, weather/fog/camera-light values. */
    public static void uploadCommon(GlShaderProgram program, SceneUniformSnapshot scene,
            SceneEnvironmentSnapshot environment) {
        if (program == null) throw new NullPointerException("program");
        if (scene == null) throw new NullPointerException("scene");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer modelView = stack.mallocFloat(16);
            modelView.put(scene.modelViewMatrix()).flip();
            program.setUniformMatrix4("gbufferModelView", modelView);

            FloatBuffer projection = stack.mallocFloat(16);
            projection.put(scene.projectionMatrix()).flip();
            program.setUniformMatrix4("gbufferProjection", projection);
        }

        program.setUniform3f("cameraPosition",
                (float) scene.cameraX(), (float) scene.cameraY(), (float) scene.cameraZ());
        program.setUniform1f("viewWidth", scene.viewportWidth());
        program.setUniform1f("viewHeight", scene.viewportHeight());
        program.setUniform1f("frameTimeCounter", (float) (scene.timeSeconds() % 3600.0d));
        program.setUniform1i("worldTime", (int) scene.normalizedDayTime());
        program.setUniform1i("worldDay", (int) Math.floorDiv(scene.dayTime(), 24000L));

        if (environment != null) {
            program.setUniform3f("fogColor", environment.fogRed(),
                    environment.fogGreen(), environment.fogBlue());
            program.setUniform1f("rainStrength", environment.rainStrength());
            program.setUniform1f("wetness", environment.rainStrength());
            program.setUniform1f("thunderStrength", environment.thunderStrength());
            program.setUniform1f("skyDarken", environment.skyDarken());
            program.setUniform1i("blockLightLevel", environment.blockLight());
            program.setUniform1i("skyLightLevel", environment.skyLight());
        }
    }
}
