package dev.opticore.client.shader;

import java.util.List;
import java.util.Set;

public final class ShaderOptionParserSelfTest {
    private ShaderOptionParserSelfTest() {}

    public static void main(String[] args) {
        String source = """
                #define BLOOM // Bloom effect
                #ifdef BLOOM
                #endif
                // #define OLD_LIGHT // Legacy lighting
                #ifndef OLD_LIGHT
                #endif
                #define QUALITY 2 // [0 1 2 3] Quality level
                const float shadowDistance = 64.0; // [32.0 64.0 128.0]
                #define IGNORED 1
                """;
        ShaderOptionParser.Summary parsed = ShaderOptionParser.parse(
                List.of(new ShaderOptionParser.Source("composite.fsh", source)),
                Set.of("QUALITY"));
        check(parsed.scannedFiles() == 1, "one shader source scanned");
        check(parsed.options().get("BLOOM").type() == ShaderOptionParser.Type.BOOLEAN,
                "ifdef-backed boolean detected");
        check(parsed.options().get("BLOOM").defaultValue().equals("true"),
                "active boolean defaults on");
        check(parsed.options().get("OLD_LIGHT").defaultValue().equals("false"),
                "commented boolean defaults off");
        check(parsed.options().get("QUALITY").values().equals(List.of("0", "1", "2", "3")),
                "numeric macro values parsed");
        check(parsed.options().get("QUALITY").slider(), "slider metadata applied");
        check(parsed.options().get("QUALITY").description().equals("Quality level"),
                "option description parsed");
        check(parsed.options().get("shadowDistance").defaultValue().equals("64.0"),
                "const option parsed");
        check(!parsed.options().containsKey("IGNORED"), "unreferenced boolean-like macro ignored");

        var conflict = ShaderOptionParser.parse(List.of(
                new ShaderOptionParser.Source("a.fsh", "#define MODE 1 // [1 2]"),
                new ShaderOptionParser.Source("b.fsh", "#define MODE 2 // [1 2]")), Set.of());
        check(conflict.options().get("MODE").ambiguous(), "conflicting defaults disabled");

        var overflow = ShaderOptionParser.parse(List.of(
                new ShaderOptionParser.Source("large.fsh", "x".repeat(ShaderOptionParser.MAX_FILE_CHARS + 1))),
                Set.of());
        check(overflow.truncated(), "oversized shader source rejected");
        System.out.println("ShaderOptionParserSelfTest: 11 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
