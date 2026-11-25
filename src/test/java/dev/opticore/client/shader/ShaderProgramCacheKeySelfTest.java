package dev.opticore.client.shader;

import java.nio.file.Path;
import java.util.Map;

/** Regression checks for deterministic and collision-safe shader program cache identities. */
public final class ShaderProgramCacheKeySelfTest {
    private ShaderProgramCacheKeySelfTest() {}

    public static void main(String[] args) {
        Path path = Path.of("shaderpacks", "Example");
        String first = ShaderProgramCacheKey.create(path, "gbuffers_terrain",
                Map.of("QUALITY", "Aa", "SHADOWS", "true"));
        String reordered = ShaderProgramCacheKey.create(path, "gbuffers_terrain",
                Map.of("SHADOWS", "true", "QUALITY", "Aa"));
        check(first.equals(reordered), "override insertion order does not change identity");

        String collisionCandidate = ShaderProgramCacheKey.create(path, "gbuffers_terrain",
                Map.of("QUALITY", "BB", "SHADOWS", "true"));
        check(!first.equals(collisionCandidate),
                "different values with colliding String hashes have distinct identities");
        check(!first.equals(ShaderProgramCacheKey.create(path, "gbuffers_water",
                Map.of("QUALITY", "Aa", "SHADOWS", "true"))), "program id participates in identity");
        check(!first.equals(ShaderProgramCacheKey.create(Path.of("shaderpacks", "Other"),
                "gbuffers_terrain", Map.of("QUALITY", "Aa", "SHADOWS", "true"))),
                "pack path participates in identity");
        System.out.println("ShaderProgramCacheKeySelfTest: 5 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
