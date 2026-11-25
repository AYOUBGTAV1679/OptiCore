package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small render-thread-only LRU owner for compiled OpenGL shader programs.
 * Entries are keyed by caller-provided program identity and exact source text.
 */
public final class GlShaderProgramCache {
    private static final int MAX_PROGRAMS = 32;
    private static final LinkedHashMap<String, Entry> PROGRAMS =
            new LinkedHashMap<>(16, 0.75f, true);

    private GlShaderProgramCache() {}

    /**
     * Returns a cached program when both source strings match exactly;
     * otherwise compiles a replacement before releasing the previous entry.
     */
    public static GlShaderProgram getOrCompile(String key, String vertexSource, String fragmentSource) {
        RenderSystem.assertOnRenderThread();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Shader program cache key must not be blank");
        }
        Entry current = PROGRAMS.get(key);
        if (current != null && current.vertexSource.equals(vertexSource)
                && current.fragmentSource.equals(fragmentSource) && !current.program.isClosed()) {
            return current.program;
        }

        // Compile first so a failed replacement does not discard the last valid program.
        GlShaderProgram replacement = GlShaderProgram.compileAndLink(key, vertexSource, fragmentSource);
        Entry previous = PROGRAMS.put(key, new Entry(vertexSource, fragmentSource, replacement));
        if (previous != null) previous.program.close();
        trimToBudget();
        return replacement;
    }

    private static void trimToBudget() {
        while (PROGRAMS.size() > MAX_PROGRAMS) {
            Map.Entry<String, Entry> eldest = PROGRAMS.entrySet().iterator().next();
            PROGRAMS.remove(eldest.getKey());
            eldest.getValue().program.close();
        }
    }

    /** Releases every owned program. Must run with a current GL context on the render thread. */
    public static void clear() {
        RenderSystem.assertOnRenderThread();
        for (Entry entry : PROGRAMS.values()) {
            entry.program.close();
        }
        PROGRAMS.clear();
    }

    public static int size() {
        RenderSystem.assertOnRenderThread();
        return PROGRAMS.size();
    }

    private record Entry(String vertexSource, String fragmentSource, GlShaderProgram program) {}
}
