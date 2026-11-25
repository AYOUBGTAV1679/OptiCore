package dev.opticore.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Minimal fullscreen scene-color composite. The source is always a separate
 * OptiCore target, so the pass never samples from the framebuffer it writes.
 * All entry points require the Minecraft render thread and a current GL context.
 */
public final class FullscreenCompositePass implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 150
            out vec2 vUv;
            void main() {
                vec2 p;
                if (gl_VertexID == 0) p = vec2(-1.0, -1.0);
                else if (gl_VertexID == 1) p = vec2(3.0, -1.0);
                else p = vec2(-1.0, 3.0);
                gl_Position = vec4(p, 0.0, 1.0);
                vUv = p * 0.5 + 0.5;
            }
            """;

    private static final String FRAGMENT_SOURCE = """
            #version 150
            uniform sampler2D uSceneColor;
            uniform sampler2D uSceneDepth;
            uniform float uWarmth;
            uniform float uFogStrength;
            uniform mat4 uInverseProjection;
            in vec2 vUv;
            out vec4 fragColor;
            void main() {
                vec4 scene = texture(uSceneColor, vUv);
                vec3 warmed = scene.rgb * vec3(1.0 + uWarmth, 1.0, 1.0 - uWarmth);
                float depth = texture(uSceneDepth, vUv).r;
                // Reconstruct view-space position so fog distance is linear.
                vec4 clipPosition = vec4(vUv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
                vec4 viewPosition = uInverseProjection * clipPosition;
                float viewDistance = abs(viewPosition.z / max(abs(viewPosition.w), 1.0e-6));
                float farHaze = smoothstep(48.0, 192.0, viewDistance) * uFogStrength;
                vec3 graded = mix(scene.rgb, warmed, 0.35);
                vec3 fogColor = vec3(0.62, 0.70, 0.78);
                fragColor = vec4(mix(graded, fogColor, farHaze), scene.a);
            }
            """;

    private GlShaderProgram program;
    private int vao;
    private boolean closed;

    public void render(GlRenderTarget scene, RenderTarget destination, float warmth,
            float fogStrength, float[] inverseProjectionMatrix) {
        RenderSystem.assertOnRenderThread();
        if (closed) throw new IllegalStateException("Composite pass is closed");
        if (scene == null || destination == null) throw new IllegalArgumentException("Scene and destination are required");
        if (scene.framebufferId() == destination.frameBufferId) {
            throw new IllegalArgumentException("Composite source and destination must be different framebuffers");
        }
        if (scene.width() != destination.width || scene.height() != destination.height) {
            throw new IllegalArgumentException("Composite source and destination dimensions must match");
        }
        if (!Float.isFinite(warmth) || warmth < 0.0f || warmth > 0.25f) {
            throw new IllegalArgumentException("Warmth must be between 0 and 0.25");
        }
        if (!Float.isFinite(fogStrength) || fogStrength < 0.0f || fogStrength > 1.0f) {
            throw new IllegalArgumentException("Fog strength must be between 0 and 1");
        }
        if (inverseProjectionMatrix == null || inverseProjectionMatrix.length != 16) {
            throw new IllegalArgumentException("Inverse projection matrix must contain 16 values");
        }
        for (float value : inverseProjectionMatrix) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Inverse projection matrix must be finite");
        }

        ensureInitialized();

        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int previousTexture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int previousTexture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        ByteBuffer colorMask = BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);

        try {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
            GL11.glViewport(0, 0, destination.width, destination.height);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDepthMask(false);
            GL11.glColorMask(true, true, true, true);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scene.colorTextureId());
            GL20.glUseProgram(program.id());
            GL20.glUniform1i(GL20.glGetUniformLocation(program.id(), "uSceneColor"), 0);
            GL20.glUniform1f(GL20.glGetUniformLocation(program.id(), "uWarmth"), warmth);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scene.depthTextureId());
            GL20.glUniform1i(GL20.glGetUniformLocation(program.id(), "uSceneDepth"), 1);
            GL20.glUniform1f(GL20.glGetUniformLocation(program.id(), "uFogStrength"), fogStrength);
            FloatBuffer inverseProjection = BufferUtils.createFloatBuffer(16);
            inverseProjection.put(inverseProjectionMatrix).flip();
            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program.id(), "uInverseProjection"),
                    false, inverseProjection);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } finally {
            GL30.glBindVertexArray(previousVao);
            GL20.glUseProgram(previousProgram);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture1);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture0);
            GL13.glActiveTexture(previousActiveTexture);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            restore(GL11.GL_BLEND, blend);
            restore(GL11.GL_DEPTH_TEST, depth);
            restore(GL11.GL_CULL_FACE, cull);
            restore(GL11.GL_SCISSOR_TEST, scissor);
            GL11.glDepthMask(depthMask);
            GL11.glColorMask(colorMask.get(0) != 0, colorMask.get(1) != 0,
                    colorMask.get(2) != 0, colorMask.get(3) != 0);
        }
    }

    private void ensureInitialized() {
        if (program != null && vao != 0) return;
        program = GlShaderProgram.compileAndLink("OptiCore fullscreen composite",
                VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            vao = GL30.glGenVertexArrays();
            if (vao == 0) throw new IllegalStateException("OpenGL could not allocate fullscreen VAO");
        } catch (RuntimeException | Error failure) {
            program.close();
            program = null;
            throw failure;
        }
    }

    private static void restore(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        vao = 0;
        if (program != null) program.close();
        program = null;
        closed = true;
    }
}
