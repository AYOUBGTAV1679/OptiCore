package dev.opticore.client.shader;

import java.nio.file.Path;
import java.util.Map;

/** Regression checks for deterministic and collision-safe shader program cache identities. */
public final class ShaderProgramCacheKeySelfTest {
    private ShaderProgramCacheKeySelfTest() {}

    public static void main(String[] args) {
        Path path = Path.of("shaderpacks", "Example");
        Map<String, String> options = Map.of("QUALITY", "Aa", "SHADOWS", "true");
        String first = ShaderProgramCacheKey.create(path, "gbuffers_terrain", options,
                "void main(){ gl_Position = vec4(1.0); }", "void main(){ }");
        String reordered = ShaderProgramCacheKey.create(path, "gbuffers_terrain",
                Map.of("SHADOWS", "true", "QUALITY", "Aa"),
                "void main(){ gl_Position = vec4(1.0); }", "void main(){ }");
        check(first.equals(reordered), "override insertion order does not change identity");

        String collisionCandidate = ShaderProgramCacheKey.create(path, "gbuffers_terrain",
                Map.of("QUALITY", "BB", "SHADOWS", "true"),
                "void main(){ gl_Position = vec4(1.0); }", "void main(){ }");
        check(!first.equals(collisionCandidate),
                "different values with colliding String hashes have distinct identities");
        check(!first.equals(ShaderProgramCacheKey.create(path, "gbuffers_water", options,
                "void main(){ gl_Position = vec4(1.0); }", "void main(){ }")),
                "program id participates in identity");
        check(!first.equals(ShaderProgramCacheKey.create(Path.of("shaderpacks", "Other"),
                "gbuffers_terrain", options,
                "void main(){ gl_Position = vec4(1.0); }", "void main(){ }")),
                "pack path participates in identity");
        check(!first.equals(ShaderProgramCacheKey.create(path, "gbuffers_terrain", options,
                "void main(){ gl_Position = vec4(0.5); }", "void main(){ }")),
                "vertex source changes invalidate the program identity");
        check(!first.equals(ShaderProgramCacheKey.create(path, "gbuffers_terrain", options,
                "void main(){ gl_Position = vec4(1.0); }", "void main(){ discard; }")),
                "fragment source changes invalidate the program identity");
        System.out.println("ShaderProgramCacheKeySelfTest: 6 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
