package dev.opticore.client.shader;

public final class ShaderStageInterfaceValidatorSelfTest {
    private ShaderStageInterfaceValidatorSelfTest() {}
    public static void main(String[] args) {
        var vertex = ShaderGlslInterfaceScanner.inspect("""
                out vec3 Color;
                out vec2 TexCoord;
                out float WrongType;
                """);
        var fragment = ShaderGlslInterfaceScanner.inspect("""
                in vec3 Color;
                in vec2 TexCoord;
                in vec4 WrongType;
                in vec3 Missing;
                """);
        var result = ShaderStageInterfaceValidator.compare(vertex, fragment);
        check(result.matched().size() == 2, "matching declarations");
        check(result.mismatches().size() == 2, "missing and mismatched declarations");
        check(result.mismatches().stream().anyMatch(m -> m.name().equals("Missing")
                && m.reason().contains("no matching")), "missing output diagnostic");
        check(result.mismatches().stream().anyMatch(m -> m.name().equals("WrongType")
                && m.reason().contains("type/array")), "type mismatch diagnostic");

        var arrayVertex = ShaderGlslInterfaceScanner.inspect("out vec2 Data[3];");
        var arrayFragment = ShaderGlslInterfaceScanner.inspect("in vec2 Data[4];");
        check(!ShaderStageInterfaceValidator.compare(arrayVertex, arrayFragment)
                .compatibleByInventory(), "array size mismatch");
        check(ShaderStageInterfaceValidator.compare(null, null).compatibleByInventory(),
                "empty summaries");

        var duplicateVertex = ShaderGlslInterfaceScanner.inspect(
                "out vec3 Repeated;\nout vec2 Repeated;");
        var duplicateFragment = ShaderGlslInterfaceScanner.inspect("in vec3 Repeated;");
        var duplicateResult = ShaderStageInterfaceValidator.compare(duplicateVertex, duplicateFragment);
        check(!duplicateResult.compatibleByInventory(), "duplicate vertex output rejected");
        check(duplicateResult.mismatches().stream().anyMatch(m -> m.name().equals("Repeated")
                && m.reason().contains("more than once")), "duplicate output diagnostic");

        var duplicateInputs = ShaderGlslInterfaceScanner.inspect(
                "in vec3 Repeated;\nin vec2 Repeated;");
        var duplicateInputResult = ShaderStageInterfaceValidator.compare(
                ShaderGlslInterfaceScanner.inspect("out vec3 Repeated;"), duplicateInputs);
        check(!duplicateInputResult.compatibleByInventory(), "duplicate fragment input rejected");
        check(duplicateInputResult.mismatches().stream().anyMatch(m -> m.name().equals("Repeated")
                && m.reason().contains("more than once")), "duplicate input diagnostic");
        var qualifiedVertex = ShaderGlslInterfaceScanner.inspect(
                "layout(location = 0) flat out highp vec3 Qualified;");
        var qualifiedFragment = ShaderGlslInterfaceScanner.inspect(
                "layout(location = 0) flat in highp vec3 Qualified;");
        check(ShaderStageInterfaceValidator.compare(qualifiedVertex, qualifiedFragment)
                .compatibleByInventory(), "layout and interpolation qualifiers");

        var commentedVertex = ShaderGlslInterfaceScanner.inspect(
                "// out vec3 Ghost;\n/* out vec4 Hidden; */\nout vec2 Visible;");
        var commentedFragment = ShaderGlslInterfaceScanner.inspect("in vec2 Visible;");
        var commentedResult = ShaderStageInterfaceValidator.compare(commentedVertex, commentedFragment);
        check(commentedResult.compatibleByInventory()
                && commentedResult.matched().contains("Visible")
                && !commentedResult.matched().contains("Ghost")
                && !commentedResult.matched().contains("Hidden"),
                "comments excluded from interface inventory");

        String oversizedSource = " ".repeat(4 * 1024 * 1024 + 1);
        var truncated = ShaderGlslInterfaceScanner.inspect(oversizedSource);
        var truncatedResult = ShaderStageInterfaceValidator.compare(
                truncated, ShaderGlslInterfaceScanner.inspect("in vec3 Any;"));
        check(truncatedResult.vertexTruncated(), "oversized vertex source marked truncated");
        check(!truncatedResult.compatibleByInventory(), "truncated inventory rejected");
        System.out.println("ShaderStageInterfaceValidatorSelfTest: 14 checks passed");
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
