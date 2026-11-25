package dev.opticore.client.shader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Bounded, Minecraft-independent lexical inspection of shader-pack property declarations. */
public final class ShaderPropertiesParser {
    public static final int MAX_PROPERTIES_BYTES = 256 * 1024;
    private static final int MAX_CONTROLS = 512;
    private static final int MAX_NESTED_SCREENS = 64;

    private ShaderPropertiesParser() {}

    public record Summary(boolean hasProperties, int propertyCount, int sliderCount,
                          String screenDefinition, List<String> controls,
                          Map<String, List<String>> nestedScreens) {
        public Summary {
            controls = List.copyOf(controls);
            Map<String, List<String>> copy = new LinkedHashMap<>();
            nestedScreens.forEach((name, entries) -> copy.put(name, List.copyOf(entries)));
            nestedScreens = java.util.Collections.unmodifiableMap(copy);
        }

        public static Summary empty() {
            return new Summary(false, 0, 0, "", List.of(), Map.of());
        }
    }

    /** Returns the exact option identifiers listed by the pack's sliders property. */
    public static Set<String> parseSliderIds(byte[] bytes) {
        if (bytes == null || bytes.length > MAX_PROPERTIES_BYTES) return Set.of();
        Properties properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(bytes));
            Set<String> ids = new LinkedHashSet<>();
            addTokens(ids, properties.getProperty("sliders", "").trim());
            return Set.copyOf(ids);
        } catch (IOException | IllegalArgumentException ignored) {
            return Set.of();
        }
    }

    public static Summary parse(byte[] bytes) {
        if (bytes == null || bytes.length > MAX_PROPERTIES_BYTES) return Summary.empty();
        Properties properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(bytes));
            String screen = properties.getProperty("screen", "").trim();
            String sliders = properties.getProperty("sliders", "").trim();
            Set<String> controls = new LinkedHashSet<>();
            addTokens(controls, screen);
            addTokens(controls, sliders);
            Map<String, List<String>> nested = new LinkedHashMap<>();
            for (String key : properties.stringPropertyNames().stream().sorted().toList()) {
                if (!key.startsWith("screen.") || nested.size() >= MAX_NESTED_SCREENS) continue;
                String name = key.substring("screen.".length()).trim();
                if (name.isEmpty()) continue;
                Set<String> entries = new LinkedHashSet<>();
                addTokens(entries, properties.getProperty(key, "").trim());
                if (!entries.isEmpty()) {
                    List<String> declared = List.copyOf(entries);
                    nested.put(name, declared);
                    addTokens(controls, String.join(" ", declared));
                }
            }
            controls.removeIf(token -> token.startsWith("[") || token.startsWith("]"));
            return new Summary(true, properties.size(), countTokens(sliders), screen,
                    new ArrayList<>(controls), nested);
        } catch (IOException | IllegalArgumentException ignored) {
            return Summary.empty();
        }
    }

    private static int countTokens(String value) {
        if (value.isBlank()) return 0;
        int count = 0;
        for (String token : value.split("[,\\s]+")) {
            if (!token.isBlank() && !token.startsWith("[") && !token.startsWith("]")) count++;
        }
        return count;
    }

    private static void addTokens(Set<String> output, String value) {
        for (String token : value.split("[,\\s]+")) {
            String normalized = token.trim();
            if (normalized.isEmpty() || normalized.startsWith("[") || normalized.startsWith("]")) continue;
            if (normalized.equals("<empty>") || normalized.equals("profile")) continue;
            if (output.size() >= MAX_CONTROLS) return;
            output.add(normalized);
        }
    }
}
