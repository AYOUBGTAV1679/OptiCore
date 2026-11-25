package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Executes an explicitly requested depth-only shadow pass.
 *
 * <p>This is an opt-in render-thread primitive, not a hook into Minecraft's
 * world renderer. The caller supplies caster drawing and must bind/upload its
 * own compatible shader. A failed pass reports failure so the caller can keep
 * the vanilla frame instead of sampling an incomplete shadow map.</p>
 */
public final class ShadowPass {
    private ShadowPass() {}

    @FunctionalInterface
    public interface CasterRenderer {
        void renderCasters(float[] lightViewProjection) throws Exception;
    }

    public static Result execute(ShadowDepthTarget target, float[] lightView,
                                 float[] lightProjection, CasterRenderer renderer) {
        RenderSystem.assertOnRenderThread();
        if (target == null || renderer == null) {
            throw new IllegalArgumentException("Shadow target and caster renderer are required");
        }
        validateMatrix(lightView, "light view");
        validateMatrix(lightProjection, "light projection");

        float[] lightViewProjection = ShadowCameraMatrices.multiply(lightProjection, lightView);
        GlState state = GlState.capture();
        try (ShadowDepthTarget.Binding ignored = target.bind()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glColorMask(false, false, false, false);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_FRONT);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(2.0f, 4.0f);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glClearDepth(1.0);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            renderer.renderCasters(lightViewProjection.clone());
            return Result.success(lightViewProjection);
        } catch (Exception failure) {
            String message = failure.getMessage();
            return Result.failure(message == null ? failure.getClass().getSimpleName() : message);
        } finally {
            state.restore();
        }
    }

    private static void validateMatrix(float[] matrix, String label) {
        if (matrix == null || matrix.length != 16) {
            throw new IllegalArgumentException(label + " matrix must contain 16 values");
        }
        for (float value : matrix) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException(label + " matrix is non-finite");
        }
    }

    public static final class Result {
        private final boolean successful;
        private final String diagnostic;
        private final float[] lightViewProjection;

        private Result(boolean successful, String diagnostic, float[] matrix) {
            this.successful = successful;
            this.diagnostic = diagnostic;
            this.lightViewProjection = matrix == null ? null : matrix.clone();
        }
        static Result success(float[] matrix) { return new Result(true, "", matrix); }
        static Result failure(String diagnostic) { return new Result(false, diagnostic, null); }
        public boolean successful() { return successful; }
        public String diagnostic() { return diagnostic; }
        public float[] lightViewProjection() {
            return lightViewProjection == null ? null : lightViewProjection.clone();
        }
    }

    /** Captures and restores the GL state modified by this pass. */
    private static final class GlState {
        private final boolean depthTest, blend, cull, polygonOffset, scissor;
        private final int depthFunc, cullFace, program;
        private final boolean depthMask;
        private final boolean[] colorMask = new boolean[4];
        private final float polygonFactor, polygonUnits;

        private GlState() {
            depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            polygonOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
            scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
            program = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            ByteBuffer mask = org.lwjgl.BufferUtils.createByteBuffer(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            for (int i = 0; i < 4; i++) colorMask[i] = mask.get(i) != 0;
            FloatBuffer offset = org.lwjgl.BufferUtils.createFloatBuffer(2);
            GL11.glGetFloatv(GL11.GL_POLYGON_OFFSET_FACTOR, offset);
            GL11.glGetFloatv(GL11.GL_POLYGON_OFFSET_UNITS, offset.position(1));
            polygonFactor = offset.get(0);
            polygonUnits = offset.get(1);
        }

        static GlState capture() { return new GlState(); }

        void restore() {
            GL11.glDepthFunc(depthFunc);
            GL11.glDepthMask(depthMask);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GL11.glCullFace(cullFace);
            GL11.glPolygonOffset(polygonFactor, polygonUnits);
            setEnabled(GL11.GL_DEPTH_TEST, depthTest);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_CULL_FACE, cull);
            setEnabled(GL11.GL_POLYGON_OFFSET_FILL, polygonOffset);
            setEnabled(GL11.GL_SCISSOR_TEST, scissor);
            GL20.glUseProgram(program);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) GL11.glEnable(capability); else GL11.glDisable(capability);
        }
    }
}
