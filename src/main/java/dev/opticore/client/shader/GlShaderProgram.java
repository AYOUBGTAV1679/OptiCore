package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, Integer> uniformLocations = new HashMap<>();
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
            // The caller's finally block owns shader cleanup on both success and failure.
            // Deleting here as well would issue a second delete for the same shader name.
            String log = GL20.glGetShaderInfoLog(shader, 16_384);
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

    /** Binds this program without capturing previous state. Prefer bindScoped(). */
    public void use() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        GL20.glUseProgram(program);
    }

    /** Binds this program and restores the previously active program on close. */
    public Binding bindScoped() {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        int previousProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(program);
        return new Binding(previousProgram);
    }

    /** Restores the default program; use only when zero is the intended prior state. */
    public static void unbind() {
        RenderSystem.assertOnRenderThread();
        GL20.glUseProgram(0);
    }

    /** Uploads a scalar integer uniform (also suitable for sampler unit indices). */
    public void setUniform1i(String name, int value) {
        int location = requireUniform(name);
        if (location >= 0) GL20.glUniform1i(location, value);
    }

    public void setUniform1f(String name, float value) {
        int location = requireUniform(name);
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    public void setUniform2f(String name, float x, float y) {
        int location = requireUniform(name);
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    public void setUniform3f(String name, float x, float y, float z) {
        int location = requireUniform(name);
        if (location >= 0) GL20.glUniform3f(location, x, y, z);
    }

    public void setUniform4f(String name, float x, float y, float z, float w) {
        int location = requireUniform(name);
        if (location >= 0) GL20.glUniform4f(location, x, y, z, w);
    }

    /** Uploads one column-major 4x4 matrix from the next 16 floats in the buffer. */
    public void setUniformMatrix4(String name, FloatBuffer matrix) {
        if (matrix == null || matrix.remaining() < 16) {
            throw new IllegalArgumentException("A 4x4 matrix requires at least 16 remaining floats");
        }
        int location = requireUniform(name);
        if (location >= 0) GL20.glUniformMatrix4fv(location, false, matrix);
    }

    /** Uploads one column-major 3x3 matrix from the next 9 floats in the buffer. */
    public void setUniformMatrix3(String name, FloatBuffer matrix) {
        if (matrix == null || matrix.remaining() < 9) {
            throw new IllegalArgumentException("A 3x3 matrix requires at least 9 remaining floats");
        }
        int location = requireUniform(name);
        if (location >= 0) GL20.glUniformMatrix3fv(location, false, matrix);
    }

    private int requireUniform(String name) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (name == null || name.isBlank() || name.length() > 256) {
            throw new IllegalArgumentException("Uniform name must contain 1-256 characters");
        }
        if (GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM) != program) {
            throw new IllegalStateException("Shader program must be bound before setting uniforms");
        }
        Integer cached = uniformLocations.get(name);
        if (cached != null) return cached;
        int location = GL20.glGetUniformLocation(program, name);
        uniformLocations.put(name, location);
        return location;
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

    /** Scoped GL_CURRENT_PROGRAM binding with idempotent restoration. */
    public static final class Binding implements AutoCloseable {
        private final int previousProgram;
        private boolean closed;

        private Binding(int previousProgram) {
            this.previousProgram = previousProgram;
        }

        @Override
        public void close() {
            RenderSystem.assertOnRenderThread();
            if (closed) return;
            GL20.glUseProgram(previousProgram);
            closed = true;
        }
    }
}
