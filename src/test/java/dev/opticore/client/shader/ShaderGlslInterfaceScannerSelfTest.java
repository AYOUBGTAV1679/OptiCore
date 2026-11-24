package dev.opticore.client.shader;

public final class ShaderGlslInterfaceScannerSelfTest {
    private ShaderGlslInterfaceScannerSelfTest() {}
    public static void main(String[] args) {
        String source = """
                #version 150
                // uniform float fakeComment;
                uniform mat4 ModelViewMatrix;
                uniform sampler2D gtexture;
                uniform highp usampler2D lightData;
                uniform float values[4];
                in vec3 Position;
                layout(location = 0) out vec4 fragColor;
                const char *fake = "uniform float fakeString;";
                void main() { }
                """;
        ShaderGlslInterfaceScanner.Summary summary = ShaderGlslInterfaceScanner.inspect(source);
        check(summary.uniforms().size() == 4, "uniform declarations");
        check(summary.samplers().size() == 2, "sampler and image declarations");
        check(summary.inputs().size() == 1 && summary.inputs().get(0).name().equals("Position"),
                "stage input");
        check(summary.outputs().size() == 1 && summary.outputs().get(0).name().equals("fragColor"),
                "layout-qualified stage output");
        check(summary.uniforms().stream().anyMatch(d -> d.name().equals("values") && d.arraySuffix().equals("[4]")),
                "uniform array");
        check(ShaderGlslInterfaceScanner.inspect("uniform float same; uniform vec3 same;")
                .duplicateUniformNames().contains("same"), "duplicate uniform names");
        check(ShaderGlslInterfaceScanner.inspect(null).uniforms().isEmpty(), "null input");
        System.out.println("ShaderGlslInterfaceScannerSelfTest: 7 checks passed");
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
