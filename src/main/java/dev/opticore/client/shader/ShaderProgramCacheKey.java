package dev.opticore.client.shader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Builds an unambiguous, deterministic identity for a shader-pack program variant. */
public final class ShaderProgramCacheKey {
    private ShaderProgramCacheKey() {}

    public static String create(Path packPath, String programId, Map<String, String> overrides) {
        return create(packPath, programId, overrides, "", "");
    }

    /**
     * Includes the processed stage sources so edits to a pack cannot reuse a stale
     * linked program merely because its path and option values stayed unchanged.
     */
    public static String create(Path packPath, String programId, Map<String, String> overrides,
                               String vertexSource, String fragmentSource) {
        Objects.requireNonNull(packPath, "packPath");
        Objects.requireNonNull(programId, "programId");
        Objects.requireNonNull(overrides, "overrides");
        Objects.requireNonNull(vertexSource, "vertexSource");
        Objects.requireNonNull(fragmentSource, "fragmentSource");
        StringBuilder key = new StringBuilder("opticore-shader-v2");
        append(key, packPath.toAbsolutePath().normalize().toString());
        append(key, programId);
        TreeMap<String, String> sorted = new TreeMap<>(overrides);
        key.append(sorted.size()).append(':');
        sorted.forEach((name, value) -> {
            append(key, name);
            append(key, Objects.requireNonNull(value, "override value"));
        });
        append(key, sourceFingerprint(vertexSource, fragmentSource));
        return key.toString();
    }

    private static String sourceFingerprint(String vertexSource, String fragmentSource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, vertexSource);
            update(digest, fragmentSource);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void update(MessageDigest digest, String source) {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static void append(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value);
    }
}
