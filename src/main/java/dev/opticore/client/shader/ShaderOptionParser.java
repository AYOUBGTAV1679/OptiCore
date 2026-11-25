package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded lexical reader for OptiFine/Iris-style shader options.
 * It discovers declarations only; it does not rewrite sources or activate options.
 */
public final class ShaderOptionParser {
    public static final int MAX_FILES = 256;
    public static final int MAX_FILE_CHARS = 512 * 1024;
    public static final int MAX_TOTAL_CHARS = 4 * 1024 * 1024;
    public static final int MAX_OPTIONS = 512;
    private static final Pattern DEFINE = Pattern.compile(
            "^\\s*(//\\s*)?#\\s*define\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s+([^/\\s]+))?\\s*(?://\\s*(.*))?$");
    private static final Pattern CONDITIONAL = Pattern.compile(
            "^\\s*#\\s*(?:ifdef|ifndef)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern CONSTANT = Pattern.compile(
            "^\\s*const\\s+(?:int|float|bool)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*([^;]+);\\s*(?://\\s*(.*))?$");
    private static final Pattern VALUE_LIST = Pattern.compile("^\\s*\\[([^]]+)](?:\\s*(.*))?$");

    private ShaderOptionParser() {}

    public enum Type { BOOLEAN, ENUM, NUMBER }

    public record Source(String name, String text) {}
    public record Option(String id, Type type, String defaultValue, List<String> values,
                         String description, boolean ambiguous, boolean slider) {
        public Option {
            values = List.copyOf(values);
        }
        public boolean editable() { return !ambiguous && (type == Type.BOOLEAN || !values.isEmpty()); }
    }
    public record Summary(Map<String, Option> options, boolean truncated, int scannedFiles) {
        public Summary {
            options = Map.copyOf(options);
        }
        public static Summary empty() { return new Summary(Map.of(), false, 0); }
    }

    public static Summary parse(List<Source> sources, Set<String> sliderIds) {
        if (sources == null || sources.isEmpty()) return Summary.empty();
        Map<String, Option> merged = new LinkedHashMap<>();
        int files = 0;
        int totalChars = 0;
        boolean truncated = false;
        for (Source source : sources) {
            if (source == null || source.text() == null || source.name() == null) continue;
            if (files >= MAX_FILES || source.text().length() > MAX_FILE_CHARS
                    || source.text().length() > MAX_TOTAL_CHARS - totalChars) {
                truncated = true;
                break;
            }
            files++;
            totalChars += source.text().length();
            Map<String, Option> found = parseOne(source.text(), sliderIds);
            for (Option option : found.values()) {
                Option previous = merged.get(option.id());
                if (previous == null) {
                    if (merged.size() < MAX_OPTIONS) merged.put(option.id(), option);
                    else truncated = true;
                } else if (!sameDefinition(previous, option)) {
                    merged.put(option.id(), new Option(option.id(), option.type(),
                            previous.defaultValue(), List.of(), option.description(),
                            true, option.slider()));
                } else if (previous.ambiguous() || option.ambiguous()) {
                    merged.put(option.id(), new Option(option.id(), option.type(),
                            previous.defaultValue(), previous.values(), previous.description(),
                            true, previous.slider() || option.slider()));
                }
            }
        }
        return new Summary(merged, truncated, files);
    }

    private static Map<String, Option> parseOne(String source, Set<String> sliders) {
        Set<String> conditional = new LinkedHashSet<>();
        String[] lines = source.split("\\R", -1);
        for (String line : lines) {
            Matcher matcher = CONDITIONAL.matcher(line);
            if (matcher.find()) conditional.add(matcher.group(1));
        }
        Map<String, Option> found = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher define = DEFINE.matcher(line);
            if (define.matches()) {
                boolean commented = define.group(1) != null;
                String id = define.group(2);
                String value = define.group(3);
                String comment = define.group(4) == null ? "" : define.group(4).trim();
                if (value == null) {
                    if (conditional.contains(id)) {
                        add(found, new Option(id, Type.BOOLEAN,
                                commented ? "false" : "true", List.of("false", "true"),
                                comment, false, false));
                    }
                } else {
                    List<String> values = parseValues(comment);
                    if (!values.isEmpty()) {
                        if (!values.contains(value)) values = append(values, value);
                        add(found, new Option(id, isNumericValues(values) ? Type.NUMBER : Type.ENUM,
                                value, values, description(comment),
                                false, sliders != null && sliders.contains(id)));
                    }
                }
                continue;
            }
            Matcher constant = CONSTANT.matcher(line);
            if (constant.matches()) {
                String comment = constant.group(3) == null ? "" : constant.group(3).trim();
                List<String> values = parseValues(comment);
                if (!values.isEmpty()) {
                    String id = constant.group(1);
                    String value = constant.group(2).trim();
                    if (!values.contains(value)) values = append(values, value);
                    add(found, new Option(id, isNumericValues(values) ? Type.NUMBER : Type.ENUM,
                            value, values, description(comment),
                            false, sliders != null && sliders.contains(id)));
                }
            }
        }
        return found;
    }

    private static List<String> parseValues(String comment) {
        Matcher matcher = VALUE_LIST.matcher(comment);
        if (!matcher.matches()) return List.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : matcher.group(1).trim().split("\\s+")) {
            if (value.matches("[A-Za-z0-9_.+-]{1,64}")) values.add(value);
            if (values.size() >= 64) break;
        }
        return List.copyOf(values);
    }

    private static String description(String comment) {
        Matcher matcher = VALUE_LIST.matcher(comment);
        return matcher.matches() && matcher.group(2) != null ? matcher.group(2).trim() : "";
    }

    private static boolean isNumericValues(List<String> values) {
        return !values.isEmpty() && values.stream().allMatch(
                value -> value.matches("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?"));
    }

    private static List<String> append(List<String> values, String value) {
        List<String> result = new ArrayList<>(values);
        result.add(value);
        return List.copyOf(result);
    }

    private static void add(Map<String, Option> found, Option option) {
        Option previous = found.putIfAbsent(option.id(), option);
        if (previous != null && !sameDefinition(previous, option)) {
            found.put(option.id(), new Option(option.id(), option.type(),
                    previous.defaultValue(), List.of(), option.description(), true, option.slider()));
        }
    }

    private static boolean sameDefinition(Option left, Option right) {
        return left.type() == right.type()
                && left.defaultValue().equals(right.defaultValue())
                && left.values().equals(right.values());
    }
}
