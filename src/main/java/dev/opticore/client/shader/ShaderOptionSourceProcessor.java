package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies validated shader-pack option overrides to expanded GLSL source before compilation. */
public final class ShaderOptionSourceProcessor {
    private static final int MAX_SOURCE_CHARS = 4 * 1024 * 1024;
    private ShaderOptionSourceProcessor() {}

    public record Result(String source, List<String> applied, List<String> ignored) {
        public Result {
            applied = List.copyOf(applied);
            ignored = List.copyOf(ignored);
        }
    }

    public static Result apply(String source, ShaderOptionParser.Summary declarations,
                               Map<String, String> overrides) {
        if (source == null || source.length() > MAX_SOURCE_CHARS) {
            throw new IllegalArgumentException("Shader source is missing or exceeds the 4 MiB limit");
        }
        if (declarations == null || overrides == null || overrides.isEmpty()) {
            return new Result(source, List.of(), List.of());
        }
        String result = source;
        List<String> applied = new ArrayList<>();
        List<String> ignored = new ArrayList<>();
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            ShaderOptionParser.Option option = declarations.options().get(entry.getKey());
            String value = entry.getValue();
            if (option == null || !option.editable() || value == null
                    || !option.values().contains(value)) {
                ignored.add(entry.getKey());
                continue;
            }
            String updated = applyOne(result, option, value);
            if (updated.equals(result)) {
                ignored.add(entry.getKey() + " (declaration not present in this stage)");
            } else {
                result = updated;
                applied.add(entry.getKey());
            }
        }
        return new Result(result, applied, ignored);
    }

    private static String applyOne(String source, ShaderOptionParser.Option option, String value) {
        String[] lines = source.split("\\R", -1);
        boolean changed = false;
        String id = option.id();
        for (int i = 0; i < lines.length; i++) {
            String original = lines[i];
            int first = 0;
            while (first < original.length() && Character.isWhitespace(original.charAt(first))) first++;
            String indent = original.substring(0, first);
            String body = original.substring(first);
            boolean commented = body.startsWith("//");
            if (commented) body = body.substring(2).stripLeading();
            if (!body.startsWith("#")) continue;
            body = body.substring(1).stripLeading();
            if (!body.startsWith("define")) continue;
            body = body.substring("define".length()).stripLeading();
            if (!body.startsWith(id)) continue;
            if (body.length() > id.length()
                    && (Character.isLetterOrDigit(body.charAt(id.length()))
                    || body.charAt(id.length()) == '_')) continue;
            String tail = body.substring(id.length());
            if (option.type() == ShaderOptionParser.Type.BOOLEAN) {
                boolean enabled = Boolean.parseBoolean(value);
                if (enabled) {
                    lines[i] = indent + "#define " + id + tail;
                } else {
                    lines[i] = indent + "// #define " + id + tail;
                }
            } else {
                int commentAt = tail.indexOf("//");
                String declaration = commentAt >= 0 ? tail.substring(0, commentAt) : tail;
                String comment = commentAt >= 0 ? " " + tail.substring(commentAt) : "";
                String trimmed = declaration.trim();
                String suffix = "";
                if (!trimmed.isEmpty()) {
                    int split = 0;
                    while (split < trimmed.length() && !Character.isWhitespace(trimmed.charAt(split))) split++;
                    suffix = trimmed.substring(split).stripLeading();
                }
                lines[i] = indent + "#define " + id + " " + value
                        + (suffix.isEmpty() ? "" : " " + suffix) + comment;
            }
            changed |= !lines[i].equals(original);
        }
        return changed ? String.join("\\n", lines) : source;
    }
}
