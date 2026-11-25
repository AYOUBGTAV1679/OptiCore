package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/**
 * Owns a bounded RGBA8 + depth off-screen framebuffer.
 *
 * <p>All operations require Minecraft's render thread and a current GL context.
 * This class is deliberately independent of Minecraft's main framebuffer;
 * callers must use {@link Binding} to restore the framebuffer and viewport.</p>
 */
public final class GlRenderTarget implements AutoCloseable {
    private static final int MAX_DIMENSION = 8192;
    private static final long MAX_PIXELS = 33_554_432L;

    private int framebuffer;
    private int colorTexture;
    private int depthTexture;
    private int width;
    private int height;
    private boolean closed;

    private GlRenderTarget(int framebuffer, int colorTexture, int depthTexture,
                           int width, int height) {
        this.framebuffer = framebuffer;
        this.colorTexture = colorTexture;
        this.depthTexture = depthTexture;
        this.width = width;
        this.height = height;
    }

    public static GlRenderTarget create(int width, int height) {
        RenderSystem.assertOnRenderThread();
        validateSize(width, height);
        return allocate(width, height);
    }

    /**
     * Replaces storage only after a complete replacement has been allocated.
     * If allocation fails, this target remains usable at its previous size.
     */
    public void resize(int newWidth, int newHeight) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        ensureNotBound();
        validateSize(newWidth, newHeight);
        if (width == newWidth && height == newHeight) return;

        GlRenderTarget replacement = allocate(newWidth, newHeight);
        int oldFramebuffer = framebuffer;
        int oldColor = colorTexture;
        int oldDepth = depthTexture;

        framebuffer = replacement.framebuffer;
        colorTexture = replacement.colorTexture;
        depthTexture = replacement.depthTexture;
        width = replacement.width;
        height = replacement.height;
        replacement.closed = true;
        replacement.framebuffer = 0;
        replacement.colorTexture = 0;
        replacement.depthTexture = 0;

        deleteObjects(oldFramebuffer, oldColor, oldDepth);
    }

    private static GlRenderTarget allocate(int width, int height) {
        int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        

        int fbo = 0;
        int texture = 0;
        int depth = 0;
        try {
            fbo = GL30.glGenFramebuffers();
            texture = GL11.glGenTextures();
            depth = GL11.glGenTextures();
            if (fbo == 0 || texture == 0 || depth == 0) {
                throw new IllegalStateException("OpenGL could not allocate framebuffer resources");
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depth);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT24, width, height, 0,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, 0L);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, texture, 0);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                    GL11.GL_TEXTURE_2D, depth, 0);

            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Off-screen framebuffer is incomplete: 0x"
                        + Integer.toHexString(status));
            }
            return new GlRenderTarget(fbo, texture, depth, width, height);
        } catch (RuntimeException | Error failure) {
            deleteObjects(fbo, texture, depth);
            throw failure;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }

    public Binding bind() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        return new Binding(framebuffer, width, height);
    }

    public void clear(float red, float green, float blue, float alpha, boolean clearDepth) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        java.nio.FloatBuffer previousClear = org.lwjgl.BufferUtils.createFloatBuffer(4);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, previousClear);
        double previousDepthClear = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        java.nio.ByteBuffer previousColorMask = org.lwjgl.BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, previousColorMask);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try (Binding ignored = bind()) {
            // glClear obeys scissor and write masks; neutralize them so the
            // off-screen target is initialized completely, then restore them.
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(red, green, blue, alpha);
            int mask = GL11.GL_COLOR_BUFFER_BIT;
            if (clearDepth) mask |= GL11.GL_DEPTH_BUFFER_BIT;
            GL11.glClear(mask);
        } finally {
            GL11.glClearColor(previousClear.get(0), previousClear.get(1),
                    previousClear.get(2), previousClear.get(3));
            GL11.glClearDepth(previousDepthClear);
            GL11.glColorMask(previousColorMask.get(0) != 0, previousColorMask.get(1) != 0,
                    previousColorMask.get(2) != 0, previousColorMask.get(3) != 0);
            GL11.glDepthMask(previousDepthMask);
            if (scissorWasEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /**
     * Copies a completed vanilla scene into this private target without sampling or
     * modifying Minecraft's source framebuffer. Color and depth must have matching sizes.
     */
    public void copySceneFrom(com.mojang.blaze3d.pipeline.RenderTarget source) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (source == null) throw new IllegalArgumentException("Source render target is required");
        if (!source.useDepth) throw new IllegalArgumentException("Source render target has no depth attachment");
        if (source.width != width || source.height != height) {
            throw new IllegalArgumentException("Source and destination dimensions must match");
        }
        if (source.frameBufferId == framebuffer) {
            throw new IllegalArgumentException("Cannot copy a render target into itself");
        }

        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            if (scissorEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    public int colorTextureId() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        return colorTexture;
    }

    /** Returns the sampleable depth attachment texture ID. */
    public int depthTextureId() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        return depthTexture;
    }

    public int framebufferId() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        return framebuffer;
    }

    public int width() { return width; }
    public int height() { return height; }
    public boolean isClosed() { return closed; }

    private static void validateSize(int width, int height) {
        if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                || (long) width * height > MAX_PIXELS) {
            throw new IllegalArgumentException("Framebuffer dimensions exceed the safety limits: "
                    + width + "x" + height);
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Render target is closed");
    }

    /** Prevent deletion/replacement from invalidating an active scoped framebuffer binding. */
    private void ensureNotBound() {
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        if (draw == framebuffer || read == framebuffer) {
            throw new IllegalStateException("Render target cannot be resized or closed while it is bound");
        }
    }

    private static void deleteObjects(int framebuffer, int texture, int depthTexture) {
        if (framebuffer != 0) GL30.glDeleteFramebuffers(framebuffer);
        if (texture != 0) GL11.glDeleteTextures(texture);
        if (depthTexture != 0) GL11.glDeleteTextures(depthTexture);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        ensureNotBound();
        deleteObjects(framebuffer, colorTexture, depthTexture);
        framebuffer = 0;
        colorTexture = 0;
        depthTexture = 0;
        closed = true;
    }

    /** Scoped framebuffer + viewport binding that restores the previous state. */
    public static final class Binding implements AutoCloseable {
        private final int previousDrawFramebuffer;
        private final int previousReadFramebuffer;
        private final int previousViewportX;
        private final int previousViewportY;
        private final int previousViewportWidth;
        private final int previousViewportHeight;
        private boolean closed;

        private Binding(int framebuffer, int width, int height) {
            previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            java.nio.IntBuffer viewport = org.lwjgl.BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            previousViewportX = viewport.get(0);
            previousViewportY = viewport.get(1);
            previousViewportWidth = viewport.get(2);
            previousViewportHeight = viewport.get(3);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL11.glViewport(0, 0, width, height);
        }

        @Override
        public void close() {
            RenderSystem.assertOnRenderThread();
            if (closed) return;
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL11.glViewport(previousViewportX, previousViewportY,
                    previousViewportWidth, previousViewportHeight);
            closed = true;
        }
    }
}
