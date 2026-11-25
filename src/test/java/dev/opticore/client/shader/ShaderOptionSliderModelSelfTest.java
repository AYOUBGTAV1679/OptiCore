package dev.opticore.client.shader;

import java.util.List;

public final class ShaderOptionSliderModelSelfTest {
    private ShaderOptionSliderModelSelfTest() {}

    public static void main(String[] args) {
        List<String> values = List.of("0.5", "1.0", "2.0", "4.0");
        check(ShaderOptionSliderModel.position(values, "0.5", "1.0") == 0.0,
                "first declared value maps to left edge");
        check(ShaderOptionSliderModel.position(values, "2.0", "1.0") == 2.0 / 3.0,
                "middle declared value maps to its discrete position");
        check(ShaderOptionSliderModel.position(values, "missing", "4.0") == 1.0,
                "invalid selection falls back to declared default");
        check("2.0".equals(ShaderOptionSliderModel.valueAt(values, 0.5)),
                "midpoint rounds to nearest discrete value");
        check("0.5".equals(ShaderOptionSliderModel.valueAt(values, -1.0)),
                "position clamps at lower edge");
        check("4.0".equals(ShaderOptionSliderModel.valueAt(values, 2.0)),
                "position clamps at upper edge");
        check("0.5".equals(ShaderOptionSliderModel.valueAt(values, Double.NaN)),
                "non-finite position safely falls back to first value");
        check(ShaderOptionSliderModel.position(List.of("only"), "only", "only") == 0.0,
                "single-value option has stable position");
        check("".equals(ShaderOptionSliderModel.valueAt(List.of(), 0.5)),
                "empty option values are handled safely");
        System.out.println("ShaderOptionSliderModelSelfTest: 9 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
