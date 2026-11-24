package dev.opticore.client.shader;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ShaderPackMetadataSelfTest {
    private ShaderPackMetadataSelfTest() {}

    public static void main(String[] args) {
        String source = "screen=[BASIC] shadowMapResolution !oldLighting [ADVANCED] profile\n"
                + "sliders=shadowMapResolution,shadowDistance\n"
                + "oldLighting=false\n";
        ShaderPackMetadata.Summary summary = ShaderPackMetadata.parseProperties(
                source.getBytes(StandardCharsets.ISO_8859_1));
        check(summary.hasProperties(), "properties detected");
        check(summary.propertyCount() == 3, "property count");
        check(summary.sliderCount() == 2, "slider count");
        check(summary.screenDefinition().contains("[BASIC]"), "screen declaration preserved");
        check(summary.controls().containsAll(List.of("shadowMapResolution", "!oldLighting",
                "shadowDistance")), "screen and slider identifiers collected");
        check(summary.controls().stream().filter("shadowMapResolution"::equals).count() == 1,
                "duplicate identifier deduplicated");
        check(!ShaderPackMetadata.parseProperties(new byte[256 * 1024 + 1]).hasProperties(),
                "oversized properties rejected");
        check(!ShaderPackMetadata.parseProperties("bad=\\uZZZZ".getBytes(StandardCharsets.ISO_8859_1))
                .hasProperties(), "malformed escape rejected");
        System.out.println("ShaderPackMetadataSelfTest: 8 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
