package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;

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
            uniform mat4 uInverseProjection;
            in vec2 vUv;
            out vec4 fragColor;
            void main() { fragColor = texture(uSceneColor, vUv); }
            """;

    private FullscreenShaderContractSelfTest() {}

    public static void main(String[] args) throws Exception {
        check(FullscreenShaderContract.validate(VERTEX, FRAGMENT).supported(),
                "known fullscreen contract accepted");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform sampler2D uSceneColor;", "uniform samplerCube uSceneColor;")).supported(),
                "wrong sampler type rejected");
        check(!FullscreenShaderContract.validate(VERTEX, FRAGMENT.replace(
                "uniform float uFogStrength;", "")).supported(),
                "missing required uniform rejected");
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
        System.out.println("FullscreenShaderContractSelfTest: 11 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
