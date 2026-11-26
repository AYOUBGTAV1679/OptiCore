package dev.opticore.client.shader;

import java.util.List;
import java.util.Map;

/** Regression tests for safe option override substitution before GLSL compilation. */
public final class ShaderOptionSourceProcessorSelfTest {
    private ShaderOptionSourceProcessorSelfTest() {}

    public static void main(String[] args) {
        ShaderOptionParser.Option quality = new ShaderOptionParser.Option(
                "QUALITY", ShaderOptionParser.Type.NUMBER, "1", List.of("0", "1", "2"),
                "quality", false, false);
        ShaderOptionParser.Option shadows = new ShaderOptionParser.Option(
                "SHADOWS", ShaderOptionParser.Type.BOOLEAN, "false", List.of("false", "true"),
                "shadows", false, false);
        ShaderOptionParser.Option bloom = new ShaderOptionParser.Option(
                "BLOOM", ShaderOptionParser.Type.BOOLEAN, "true", List.of("false", "true"),
                "bloom", false, false);
        ShaderOptionParser.Option filterMode = new ShaderOptionParser.Option(
                "FILTER_MODE", ShaderOptionParser.Type.ENUM, "nearest",
                List.of("nearest", "linear", "cubic"), "filter mode", false, false);
        ShaderOptionParser.Option simpleToggle = new ShaderOptionParser.Option(
                "SIMPLE_TOGGLE", ShaderOptionParser.Type.BOOLEAN, "true",
                List.of("true", "false"), "simple toggle", false, false);
        ShaderOptionParser.Summary declarations = new ShaderOptionParser.Summary(
                Map.of("QUALITY", quality, "SHADOWS", shadows, "BLOOM", bloom,
                        "FILTER_MODE", filterMode, "SIMPLE_TOGGLE", simpleToggle), false, 1);

        String source = "#version 150\n"
                + "#define QUALITY 1 // [0 1 2]\n"
                + "// #define SHADOWS // toggled\n"
                + "#define BLOOM\n";
        ShaderOptionSourceProcessor.Result result = ShaderOptionSourceProcessor.apply(source,
                declarations, Map.of("QUALITY", "2", "SHADOWS", "true", "BLOOM", "false"));
        check(result.source().contains("#define QUALITY 2"), "numeric define override");
        check(result.source().contains("#define SHADOWS"), "commented boolean enable");
        check(result.source().contains("// #define BLOOM"), "active boolean disable");
        check(result.applied().size() == 3, "all valid overrides reported");
        String inlineDeclarations = "#define SIMPLE_TOGGLE // [true false] Simple toggle\\n"
                + "#define FILTER_MODE // [nearest linear cubic] Filter mode\\n";
        ShaderOptionSourceProcessor.Result inlineApplied = ShaderOptionSourceProcessor.apply(
                inlineDeclarations, declarations,
                Map.of("SIMPLE_TOGGLE", "false", "FILTER_MODE", "cubic"));
        check(inlineApplied.source().contains("// #define SIMPLE_TOGGLE"),
                "inline boolean declaration can be disabled");
        check(inlineApplied.source().contains("#define FILTER_MODE cubic // [nearest linear cubic] Filter mode"),
                "inline enum declaration receives selected value");
        check(inlineApplied.applied().size() == 2,
                "inline boolean and enum overrides are reported as applied");
        ShaderOptionSourceProcessor.Result unchangedValid = ShaderOptionSourceProcessor.apply(
                "#define BLOOM\n", declarations, Map.of("BLOOM", "true"));
        check(unchangedValid.source().equals("#define BLOOM\n"),
                "already-matching boolean declaration remains unchanged");
        check(unchangedValid.applied().contains("BLOOM"),
                "already-matching declaration is still reported as applied");
        ShaderOptionSourceProcessor.Result absentStage = ShaderOptionSourceProcessor.apply(
                "#version 150\nvoid main() {}\n", declarations, Map.of("BLOOM", "false"));
        check(absentStage.source().equals("#version 150\nvoid main() {}\n"),
                "override absent from this shader stage leaves source unchanged");
        check(absentStage.ignored().contains("BLOOM (declaration not present in this stage)"),
                "override absent from this stage is reported explicitly");
        ShaderOptionSourceProcessor.Result unchangedNumeric = ShaderOptionSourceProcessor.apply(
                "#define QUALITY 1 // [0 1 2]\n", declarations, Map.of("QUALITY", "1"));
        check(unchangedNumeric.applied().contains("QUALITY"),
                "already-matching numeric declaration is reported as applied");

        ShaderOptionSourceProcessor.Result invalid = ShaderOptionSourceProcessor.apply(source,
                declarations, Map.of("QUALITY", "999", "UNKNOWN", "true"));
        check(invalid.source().equals(source), "invalid overrides leave source unchanged");
        check(invalid.ignored().size() == 2, "invalid overrides reported");

        Map<String, String> nullKeyOverride = new java.util.HashMap<>();
        nullKeyOverride.put(null, "true");
        nullKeyOverride.put("QUALITY", null);
        ShaderOptionSourceProcessor.Result nullEntries = ShaderOptionSourceProcessor.apply(
                source, declarations, nullKeyOverride);
        check(nullEntries.source().equals(source), "null override entries leave source unchanged");
        check(nullEntries.ignored().contains("<null>"),
                "null option key is reported without throwing");
        check(nullEntries.ignored().contains("QUALITY"),
                "null option value is reported without throwing");

        String commentedNumeric = "// #define QUALITY 1 // [0 1 2]";
        ShaderOptionSourceProcessor.Result activated = ShaderOptionSourceProcessor.apply(
                commentedNumeric, declarations, Map.of("QUALITY", "2"));
        check(activated.source().equals("#define QUALITY 2 // [0 1 2]"),
                "commented numeric option is activated");
        String constSource = "const int QUALITY = 1; // quality setting";
        ShaderOptionSourceProcessor.Result constOverride = ShaderOptionSourceProcessor.apply(
                constSource, declarations, Map.of("QUALITY", "2"));
        check(constOverride.source().equals("const int QUALITY = 2; // quality setting"),
                "const initializer override preserves declaration suffix");
        check(constOverride.applied().contains("QUALITY"), "const override reported as applied");
        String functionMacro = "#define QUALITY(x) ((x) + 1)";
        ShaderOptionSourceProcessor.Result macroResult = ShaderOptionSourceProcessor.apply(
                functionMacro, declarations, Map.of("QUALITY", "2"));
        check(macroResult.source().equals(functionMacro), "function-like macro is not rewritten");
        check(macroResult.ignored().size() == 1, "function-like macro override is reported ignored");
        System.out.println("ShaderOptionSourceProcessorSelfTest: 22 checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
