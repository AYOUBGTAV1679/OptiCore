package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FullscreenShaderContractSelfTest {
    private static final String VERTEX = """
            #version 150
            out vec2 vUv;
            void main() {
                vec2 p = gl_VertexID == 0 ? vec2(-1.0, -1.0)
                    : gl_VertexID == 1 ? vec2(3.0, -1.0) : vec2(-1.0, 3.0);
                gl_Position = vec4(p, 0.0, 1.0);
                vUv = p * 0.5 + 0.5;
            }
            """;
    private static final String FRAGMENT = """
            #version 150
            uniform sampler2D uSceneColor;
            uniform sampler2D uSceneDepth;
            uniform float uWarmth;
            uniform float uFogStrength;
            uniform vec3 uFogColor;
            uniform float uFogStart;
            uniform float uFogEnd;
            uniform float uExposure;
            uniform float uContrast;
            uniform float uSaturation;
            uniform mat4 uInverseProjection;
            in vec2 vUv;
            out vec4 fragColor;
            void main() { fragColor = texture(uSceneColor, vUv); }
            """;

    private FullscreenShaderContractSelfTest() {}

    public static void main(String[] args) throws Exception {
        check(FullscreenShaderContract.validate(VERTEX, FRAGMENT).supported(),
                "known fullscreen contract accepted");
        AtmosphericFogRange defaultFog = AtmosphericFogRange.fromConfig(Double.NaN, Double.NaN);
        check(defaultFog.start() == 48.0f && defaultFog.end() == 192.0f,
                "invalid fog settings fall back to safe defaults");
        AtmosphericFogRange invertedFog = AtmosphericFogRange.fromConfig(256.0, 96.0);
        check(invertedFog.start() == 256.0f && invertedFog.end() > invertedFog.start(),
                "inverted fog distances are corrected");
        AtmosphericFogRange boundedFog = AtmosphericFogRange.fromConfig(9000.0, 9000.0);
        check(boundedFog.start() == 2048.0f && boundedFog.end() == 4096.0f,
                "fog distances are clamped to supported bounds");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform sampler2D uSceneColor;", "uniform samplerCube uSceneColor;")).supported(),
                "wrong sampler type rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform float uFogStrength;", "")).supported(),
                "missing required uniform rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform vec3 uFogColor;", "uniform vec4 uFogColor;")).supported(),
                "wrong fog-color uniform type rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform float uFogStart;", "")).supported(),
                "missing fog start uniform rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform float uFogEnd;", "uniform vec2 uFogEnd;")).supported(),
                "wrong fog end uniform type rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform mat4 uInverseProjection;",
                "uniform mat4 uInverseProjection;\nuniform float uTime;")).supported(),
                "unsupported extra uniform rejected");
        check(!FullscreenShaderContract.validate(VERTEX.replace("gl_VertexID", "gl_VertexIndex"),
                FRAGMENT).supported(), "non-contract vertex generation rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "out vec4 fragColor;", "out vec3 fragColor;")).supported(),
                "wrong fragment output type rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "in vec2 vUv;", "in vec3 vUv;")).supported(), "varying type mismatch rejected");
        check(!FullscreenShaderContract.validate("", FRAGMENT).supported(), "missing vertex rejected");
        Path demo = Path.of("examples/opticore-fullscreen-demo/shaders");
        String demoVertex = Files.readString(demo.resolve("final.vsh"));
        String demoFragment = Files.readString(demo.resolve("final.fsh"));
        check(FullscreenShaderContract.validate(demoVertex, demoFragment).supported(),
                "checked-in demo pack satisfies the fullscreen contract");
        ShaderProgramIndex.Summary demoIndex = ShaderProgramIndex.inspect(demo.getParent(), false);
        check(demoIndex.pairedPrograms() == 1 && demoIndex.paired().contains("final"),
                "checked-in demo pack is discovered as one paired program");
        ShaderPackCompatibilityReport.Summary demoReport =
                ShaderPackCompatibilityReport.inspect(demo.getParent(), false);
        check(demoReport.readyPrograms() == 1 && demoReport.allInspectedProgramsReady(),
                "checked-in demo pack passes bounded source preflight");
        ShaderOptionParser.Summary demoOptions =
                ShaderOptionDiscovery.inspect(demo.getParent(), false);
        check(demoOptions.options().size() == 6
                        && demoOptions.options().values().stream().allMatch(ShaderOptionParser.Option::editable),
                "demo pack exposes six safe editable options");
        check(demoOptions.options().values().stream().allMatch(ShaderOptionParser.Option::slider),
                "demo pack slider metadata marks all six options as sliders");
        ShaderOptionSourceProcessor.Result overridden = ShaderOptionSourceProcessor.apply(
                demoFragment, demoOptions, Map.of("WARMTH_SCALE", "2.0", "EXPOSURE_SCALE", "1.5", "CONTRAST_SCALE", "1.25", "SATURATION_SCALE", "0.5", "FOG_NEAR", "72.0"));
        check(overridden.source().contains("#define WARMTH_SCALE 2.0")
                        && overridden.source().contains("#define FOG_NEAR 72.0")
                        && overridden.source().contains("#define EXPOSURE_SCALE 1.5")
                        && overridden.source().contains("#define CONTRAST_SCALE 1.25")
                        && overridden.source().contains("#define SATURATION_SCALE 0.5"),
                "demo warmth, exposure, contrast, saturation, and fog overrides are applied to GLSL source");
        check(FullscreenShaderContract.validate(demoVertex, overridden.source()).supported(),
                "option-adjusted demo source still satisfies fullscreen contract");

        Path demoZip = Files.createTempFile("opticore-fullscreen-demo", ".zip");
        try {
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(demoZip))) {
                output.putNextEntry(new ZipEntry("shaders/final.vsh"));
                output.write(demoVertex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
                output.putNextEntry(new ZipEntry("shaders/final.fsh"));
                output.write(demoFragment.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
                output.putNextEntry(new ZipEntry("shaders/shaders.properties"));
                output.write("sliders=WARMTH_SCALE EXPOSURE_SCALE CONTRAST_SCALE SATURATION_SCALE FOG_NEAR FOG_FAR\\nscreen=WARMTH_SCALE EXPOSURE_SCALE CONTRAST_SCALE SATURATION_SCALE FOG_NEAR FOG_FAR\\n"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
            }
            ShaderProgramIndex.Summary zipIndex = ShaderProgramIndex.inspect(demoZip, true);
            check(zipIndex.pairedPrograms() == 1 && zipIndex.paired().contains("final"),
                    "ZIP demo pack is discovered as one paired program");
            ShaderPackCompatibilityReport.Summary zipReport =
                    ShaderPackCompatibilityReport.inspect(demoZip, true);
            check(zipReport.readyPrograms() == 1 && zipReport.allInspectedProgramsReady(),
                    "ZIP demo pack passes bounded source preflight");
            ShaderOptionParser.Summary zipOptions = ShaderOptionDiscovery.inspect(demoZip, true);
            check(zipOptions.options().size() == 6,
                    "ZIP demo pack exposes all six editable options");
            check(zipOptions.options().values().stream().allMatch(ShaderOptionParser.Option::slider),
                    "ZIP slider metadata marks all six options as sliders");
        } finally {
            Files.deleteIfExists(demoZip);
        }
        System.out.println("FullscreenShaderContractSelfTest: 25 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
