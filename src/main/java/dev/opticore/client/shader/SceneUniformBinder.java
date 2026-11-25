package dev.opticore.client.shader;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/** Uploads captured scene data to an already-bound OptiCore shader program. */
public final class SceneUniformBinder {
    private SceneUniformBinder() {}

    /** Compatibility overload for callers that have only matrix and time data. */
    public static void uploadCommon(GlShaderProgram program, SceneUniformSnapshot scene) {
        uploadCommon(program, scene, null, null);
    }

    /**
     * Uploads common OptiFine-style matrix/time names and optional environment values.
     * Missing uniforms are harmless no-ops. This is a starter mapping, not full OptiFine parity.
     */
    public static void uploadCommon(GlShaderProgram program, SceneUniformSnapshot scene,
            SceneEnvironmentSnapshot environment) {
        uploadCommon(program, scene, environment, null);
    }

    /** Uploads scene, environment, and optional interpolated celestial sun-angle uniforms. */
    public static void uploadCommon(GlShaderProgram program, SceneUniformSnapshot scene,
            SceneEnvironmentSnapshot environment, DirectionalSunSnapshot sun) {
        if (program == null) throw new NullPointerException("program");
        if (scene == null) throw new NullPointerException("scene");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer modelView = stack.mallocFloat(16);
            modelView.put(scene.modelViewMatrix()).flip();
            program.setUniformMatrix4("gbufferModelView", modelView);

            FloatBuffer modelViewInverse = stack.mallocFloat(16);
            modelViewInverse.put(scene.modelViewMatrixInverse()).flip();
            program.setUniformMatrix4("gbufferModelViewInverse", modelViewInverse);

            FloatBuffer normal = stack.mallocFloat(9);
            normal.put(scene.normalMatrix()).flip();
            program.setUniformMatrix3("normalMatrix", normal);


            FloatBuffer projection = stack.mallocFloat(16);
            projection.put(scene.projectionMatrix()).flip();
            program.setUniformMatrix4("gbufferProjection", projection);

            FloatBuffer projectionInverse = stack.mallocFloat(16);
            projectionInverse.put(scene.projectionMatrixInverse()).flip();
            program.setUniformMatrix4("gbufferProjectionInverse", projectionInverse);
        }

        program.setUniform3f("cameraPosition",
                (float) scene.cameraX(), (float) scene.cameraY(), (float) scene.cameraZ());
        program.setUniform1f("viewWidth", scene.viewportWidth());
        program.setUniform1f("viewHeight", scene.viewportHeight());
        program.setUniform1f("frameTimeCounter", (float) (scene.timeSeconds() % 3600.0d));
        program.setUniform1i("worldTime", (int) scene.normalizedDayTime());
        program.setUniform1i("worldDay", (int) Math.floorDiv(scene.dayTime(), 24000L));

        if (sun != null) {
            // OptiFine-compatible scalar angle; do not treat this as a world-space direction vector.
            program.setUniform1f("sunAngle", sun.sunAngleRadians());
            program.setUniform1f("sunAngleRadians", sun.sunAngleRadians());
            // Trigonometric forms avoid repeating angle conversion in pack shaders.
            // These remain celestial-angle values, not a claimed world-space light vector.
            program.setUniform1f("sunAngleSin", sun.sunAngleSin());
            program.setUniform1f("sunAngleCos", sun.sunAngleCos());
            program.setUniform1f("sunRainStrength", sun.rain());
            program.setUniform1f("sunThunderStrength", sun.thunder());
        }

        if (environment != null) {
            program.setUniform3f("skyColor", environment.skyRed(),
                    environment.skyGreen(), environment.skyBlue());
            program.setUniform1f("rainStrength", environment.rainStrength());
            program.setUniform1f("wetness", environment.rainStrength());
            program.setUniform1f("thunderStrength", environment.thunderStrength());
            program.setUniform1f("skyDarken", environment.skyDarken());
            program.setUniform1i("blockLightLevel", environment.blockLight());
            program.setUniform1i("skyLightLevel", environment.skyLight());
        }
    }
}
