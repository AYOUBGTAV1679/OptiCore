package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded lexical inventory of common GLSL global declarations.
 * This is diagnostic metadata, not a GLSL grammar parser or linker.
 */
public final class ShaderGlslInterfaceScanner {
    private static final int MAX_SOURCE_CHARS = 4 * 1024 * 1024;
    private static final Pattern UNIFORM = Pattern.compile(
            "\\buniform\\s+(?:(?:lowp|mediump|highp)\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(\\[[^\\]]*\\])?\\s*;");
    private static final Pattern INTERFACE = Pattern.compile(
            "(?:(?:layout\\s*\\([^)]*\\)\\s*)?)(?:(?:flat|smooth|noperspective|centroid|sample|patch|invariant)\\s+)*"
                    + "\\b(in|out)\\s+(?:(?:lowp|mediump|highp)\\s+)?"
                    + "([A-Za-z_][A-Za-z0-9_]*)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(\\[[^\\]]*\\])?\\s*;");
    private ShaderGlslInterfaceScanner() {}

    public record Declaration(String type, String name, String arraySuffix) {}
    public record Summary(List<Declaration> uniforms, List<Declaration> samplers,
                          List<Declaration> inputs, List<Declaration> outputs,
                          Set<String> duplicateUniformNames, boolean truncated) {
        public Summary {
            uniforms = List.copyOf(uniforms);
            samplers = List.copyOf(samplers);
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            duplicateUniformNames = Set.copyOf(duplicateUniformNames);
        }
        public static Summary empty() {
            return new Summary(List.of(), List.of(), List.of(), List.of(), Set.of(), false);
        }
    }

    public static Summary inspect(String source) {
        if (source == null || source.isEmpty()) return Summary.empty();
        boolean truncated = source.length() > MAX_SOURCE_CHARS;
        String bounded = truncated ? source.substring(0, MAX_SOURCE_CHARS) : source;
        String code = maskCommentsAndStrings(bounded);
        List<Declaration> uniforms = new ArrayList<>(), samplers = new ArrayList<>();
        List<Declaration> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        Set<String> names = new TreeSet<>(), duplicates = new TreeSet<>();
        Matcher uniformMatcher = UNIFORM.matcher(code);
        while (uniformMatcher.find()) {
            String type = uniformMatcher.group(1), name = uniformMatcher.group(2);
            Declaration declaration = new Declaration(type, name, nullToEmpty(uniformMatcher.group(3)));
            uniforms.add(declaration);
            if (!names.add(name)) duplicates.add(name);
            String lowerType = type.toLowerCase(Locale.ROOT);
            if (lowerType.contains("sampler") || lowerType.contains("image")) samplers.add(declaration);
        }
        Matcher interfaceMatcher = INTERFACE.matcher(code);
        while (interfaceMatcher.find()) {
            Declaration declaration = new Declaration(interfaceMatcher.group(2), interfaceMatcher.group(3),
                    nullToEmpty(interfaceMatcher.group(4)));
            if ("in".equals(interfaceMatcher.group(1))) inputs.add(declaration);
            else outputs.add(declaration);
        }
        return new Summary(uniforms, samplers, inputs, outputs, duplicates, truncated);
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }

    private static String maskCommentsAndStrings(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean line = false, block = false, quoted = false, escaped = false;
        char quote = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i), next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (line) {
                if (c == '\n' || c == '\r') { line = false; out.append(c); } else out.append(' ');
            } else if (block) {
                if (c == '*' && next == '/') { out.append("  "); i++; block = false; }
                else out.append(c == '\n' || c == '\r' ? c : ' ');
            } else if (quoted) {
                if (c == '\n' || c == '\r') out.append(c); else out.append(' ');
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quoted = false;
            } else if (c == '/' && next == '/') { out.append("  "); i++; line = true; }
            else if (c == '/' && next == '*') { out.append("  "); i++; block = true; }
            else if (c == '"' || c == '\'') { out.append(' '); quoted = true; quote = c; }
            else out.append(c);
        }
        return out.toString();
    }
}
