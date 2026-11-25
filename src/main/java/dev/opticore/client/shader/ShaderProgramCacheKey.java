package dev.opticore.client.shader;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Builds an unambiguous, deterministic identity for a shader-pack program variant. */
public final class ShaderProgramCacheKey {
    private ShaderProgramCacheKey() {}

    public static String create(Path packPath, String programId, Map<String, String> overrides) {
        Objects.requireNonNull(packPath, "packPath");
        Objects.requireNonNull(programId, "programId");
        Objects.requireNonNull(overrides, "overrides");
        StringBuilder key = new StringBuilder("opticore-shader-v1");
        append(key, packPath.toAbsolutePath().normalize().toString());
        append(key, programId);
        TreeMap<String, String> sorted = new TreeMap<>(overrides);
        key.append(sorted.size()).append(':');
        sorted.forEach((name, value) -> {
            append(key, name);
            append(key, Objects.requireNonNull(value, "override value"));
        });
        return key.toString();
    }

    private static void append(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value);
    }
}
