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
        String id = Pattern.quote(option.id());
        String[] lines = source.split("\\R", -1);
        boolean changed = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (option.type() == ShaderOptionParser.Type.BOOLEAN) {
                Pattern p = Pattern.compile("^(\\s*)(//\\s*)?(#\\s*define\\s+" + id + "(?:\\s+.*)?)$");
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    boolean enabled = Boolean.parseBoolean(value);
                    String directive = m.group(3);
                    lines[i] = enabled ? m.group(1) + directive : m.group(1) + "// " + directive;
                    changed |= !lines[i].equals(line);
                }
            } else {
                Pattern define = Pattern.compile("^(\\s*)(//\\s*)?(#\\s*define\\s+" + id
                        + ")(?:\\s+[^/\\s]+)?(\\s*(?://.*)?)$");
                Matcher m = define.matcher(line);
                if (m.matches()) {
                    lines[i] = m.group(1) + (m.group(2) == null ? "" : m.group(2))
                            + m.group(3) + " " + value + m.group(4);
                    changed |= !lines[i].equals(line);
                    continue;
                }
                Pattern constant = Pattern.compile("^(\\s*const\\s+(?:int|float|bool)\\s+"
                        + id + "\\s*=\\s*)([^;]+)(;.*)$");
                Matcher c = constant.matcher(line);
                if (c.matches()) {
                    lines[i] = c.group(1) + value + c.group(3);
                    changed |= !lines[i].equals(line);
                }
            }
        }
        return changed ? String.join("\\n", lines) : source;
    }
}
