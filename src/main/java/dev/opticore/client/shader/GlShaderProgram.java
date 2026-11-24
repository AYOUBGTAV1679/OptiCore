package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL20;

/**
 * Owns one linked OpenGL vertex/fragment program.
 *
 * <p>All methods must be called on Minecraft's render thread while an OpenGL
 * context is current. This class deliberately does not install the program
 * into Minecraft's world-render pipeline; callers must restore the previous
 * GL program/state after drawing.</p>
 */
public final class GlShaderProgram implements AutoCloseable {
    private final int program;
    private boolean closed;

    private GlShaderProgram(int program) {
        this.program = program;
    }

    /**
     * Compiles and links two already-expanded GLSL sources.
     *
     * @throws IllegalArgumentException if a source is missing or exceeds the
     *                                  defensive size limit
     * @throws IllegalStateException if OpenGL compilation or linking fails
     */
    public static GlShaderProgram compileAndLink(String label, String vertexSource,
                                                  String fragmentSource) {
        RenderSystem.assertOnRenderThread();
        validateSource(label, vertexSource, "vertex");
        validateSource(label, fragmentSource, "fragment");

        int vertexShader = 0;
        int fragmentShader = 0;
        int program = 0;
        try {
            vertexShader = compileStage(label, "vertex", GL20.GL_VERTEX_SHADER, vertexSource);
            fragmentShader = compileStage(label, "fragment", GL20.GL_FRAGMENT_SHADER, fragmentSource);

            program = GL20.glCreateProgram();
            if (program == 0) {
                throw new IllegalStateException("OpenGL could not allocate a program for " + label);
            }
            GL20.glAttachShader(program, vertexShader);
            GL20.glAttachShader(program, fragmentShader);
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(program, 16_384);
                throw new IllegalStateException("Shader program link failed [" + label + "]: " + log);
            }
            GL20.glDetachShader(program, vertexShader);
            GL20.glDetachShader(program, fragmentShader);
            return new GlShaderProgram(program);
        } catch (RuntimeException | Error failure) {
            if (program != 0) {
                GL20.glDeleteProgram(program);
            }
            throw failure;
        } finally {
            if (vertexShader != 0) GL20.glDeleteShader(vertexShader);
            if (fragmentShader != 0) GL20.glDeleteShader(fragmentShader);
        }
    }

    private static int compileStage(String label, String stage, int type, String source) {
        int shader = GL20.glCreateShader(type);
        if (shader == 0) {
            throw new IllegalStateException("OpenGL could not allocate " + stage + " shader [" + label + "]");
        }
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 16_384);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Shader " + stage + " compilation failed [" + label + "]: " + log);
        }
        return shader;
    }

    private static void validateSource(String label, String source, String stage) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Missing " + stage + " source for " + label);
        }
        if (source.length() > 4 * 1024 * 1024) {
            throw new IllegalArgumentException(stage + " source exceeds the 4 MiB safety limit for " + label);
        }
    }

    /** Binds this program. The caller is responsible for restoring prior GL state. */
    public void use() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        GL20.glUseProgram(program);
    }

    /** Stops using any custom program; callers should restore their captured state instead when needed. */
    public static void unbind() {
        RenderSystem.assertOnRenderThread();
        GL20.glUseProgram(0);
    }

    public int id() {
        ensureOpen();
        return program;
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Shader program is already closed");
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        GL20.glDeleteProgram(program);
        closed = true;
    }
}
