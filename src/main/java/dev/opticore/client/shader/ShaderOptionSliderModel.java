package dev.opticore.client.shader;

import java.util.List;

/** Maps a normalized slider position to one of a shader option's declared values. */
public final class ShaderOptionSliderModel {
    private ShaderOptionSliderModel() {}

    public static double position(List<String> values, String selected, String defaultValue) {
        if (values == null || values.size() <= 1) return 0.0;
        int index = values.indexOf(selected);
        if (index < 0) index = values.indexOf(defaultValue);
        if (index < 0) index = 0;
        return (double) index / (values.size() - 1);
    }

    public static String valueAt(List<String> values, double position) {
        if (values == null || values.isEmpty()) return "";
        if (!Double.isFinite(position)) position = 0.0;
        double clamped = Math.max(0.0, Math.min(1.0, position));
        int index = (int) Math.round(clamped * (values.size() - 1));
        return values.get(index);
    }
}
