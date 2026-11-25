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
            String optionId = entry.getKey();
            String value = entry.getValue();
            // Map.of(...).get(null) throws, so reject a null key before map lookup.
            if (optionId == null) {
                ignored.add("<null>");
                continue;
            }
            ShaderOptionParser.Option option = declarations.options().get(optionId);
            if (option == null || !option.editable() || value == null
                    || !option.values().contains(value)) {
                ignored.add(optionId);
                continue;
            }
            ApplyResult update = applyOne(result, option, value);
            if (!update.found()) {
                ignored.add(optionId + " (declaration not present in this stage)");
            } else {
                result = update.source();
                applied.add(optionId);
            }
        }
        return new Result(result, applied, ignored);
    }

    private record ApplyResult(String source, boolean found) {}

    private static ApplyResult applyOne(String source, ShaderOptionParser.Option option, String value) {
        String[] lines = source.split("\\R", -1);
        boolean changed = false;
        boolean found = false;
        String id = option.id();
        for (int i = 0; i < lines.length; i++) {
            String original = lines[i];
            // OptiFine-style packs may expose editable values as GLSL constants.
            // Replace only the initializer of the exact declaration and retain its suffix.
            java.util.regex.Matcher constant = java.util.regex.Pattern.compile(
                    "^(\\s*const\\s+(?:int|float|bool)\\s+"
                            + java.util.regex.Pattern.quote(id)
                            + "\\s*=\\s*)([^;]+)(;.*)$").matcher(original);
            if (constant.matches()) {
                found = true;
                String updated = constant.group(1) + value + constant.group(3);
                if (!updated.equals(original)) {
                    lines[i] = updated;
                    changed = true;
                }
                continue;
            }
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
            // A function-like macro is a different declaration; never rewrite its name/body.
            // In GLSL, the opening parenthesis must immediately follow the macro identifier.
            if (tail.startsWith("(")) continue;
            found = true;
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
        return changed ? String.join("\n", lines) : source;
    }
}
