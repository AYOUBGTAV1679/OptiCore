package dev.opticore.client.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bounded, heuristic GLSL source preflight. This is not a GLSL parser or compiler.
 */
public final class ShaderSourcePreflight {
    private static final int MAX_ENTRIES = 20_000;
    private static final int MAX_SOURCE_FILES = 4_096;
    private static final long MAX_FILE_BYTES = 512L * 1024;
    private static final long MAX_TOTAL_BYTES = 16L * 1024 * 1024;
    private static final Pattern MAIN = Pattern.compile("\\bvoid\\s+main\\s*\\(");
    private static final Pattern VERSION = Pattern.compile("(?m)^[ \\t]*#[ \\t]*version[ \\t]+([0-9]+)(?:[ \\t]+([A-Za-z]+))?");
    private static final Pattern EXTENSION = Pattern.compile("(?m)^[ \\t]*#[ \\t]*extension[ \\t]+([A-Za-z0-9_]+)[ \\t]*:");
    private static final Pattern INCLUDE = Pattern.compile("(?m)^[ \\t]*#[ \\t]*include[ \\t]+[<\"][^>\"]+[>\"]");
    private ShaderSourcePreflight() {}

    public record Summary(int inspectedStages, int stagesWithoutMain, int versionedStages,
                          Set<String> versions, int includeDirectives, Set<String> extensions,
                          int extensionDirectives, boolean truncated) {
        public Summary {
            versions = Set.copyOf(versions);
            extensions = Set.copyOf(extensions);
        }
        public static Summary empty() {
            return new Summary(0, 0, 0, Set.of(), 0, Set.of(), 0, false);
        }
    }

    private record StageResult(boolean hasMain, String version, int includes, Set<String> extensions,
                               int extensionDirectives) {}

    public static Summary inspect(Path pack, boolean archive) {
        if (pack == null) return Summary.empty();
        try { return archive ? inspectZip(pack) : inspectFolder(pack); }
        catch (IOException | SecurityException ignored) { return Summary.empty(); }
    }

    private static Summary inspectFolder(Path pack) throws IOException {
        Path normalizedPack = pack.toAbsolutePath().normalize();
        Path root = normalizedPack.resolve("shaders").normalize();
        if (!root.startsWith(normalizedPack) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            return Summary.empty();
        int entries = 0, stages = 0, missingMain = 0, versioned = 0, includes = 0, extensionCount = 0;
        long bytes = 0;
        boolean truncated = false;
        Set<String> versions = new TreeSet<>(), extensions = new TreeSet<>();
        try (var paths = Files.walk(root, 32)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                if (++entries > MAX_ENTRIES) { truncated = true; break; }
                Path file = iterator.next();
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".vsh") && !name.endsWith(".fsh")) continue;
                if (stages >= MAX_SOURCE_FILES) { truncated = true; break; }
                long size = Files.size(file);
                if (size > MAX_FILE_BYTES || size > MAX_TOTAL_BYTES - bytes) { truncated = true; continue; }
                StageResult result;
                try (InputStream in = Files.newInputStream(file)) { result = scan(in, size); }
                bytes += size;
                stages++;
                if (!result.hasMain()) missingMain++;
                if (result.version() != null) { versioned++; versions.add(result.version()); }
                includes += result.includes();
                extensions.addAll(result.extensions());
                extensionCount += result.extensionDirectives();
            }
        }
        return new Summary(stages, missingMain, versioned, versions, includes, extensions, extensionCount, truncated);
    }

    private static Summary inspectZip(Path path) throws IOException {
        int entries = 0, stages = 0, missingMain = 0, versioned = 0, includes = 0, extensionCount = 0;
        long bytes = 0;
        boolean truncated = false;
        Set<String> versions = new TreeSet<>(), extensions = new TreeSet<>();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> iterator = zip.entries();
            while (iterator.hasMoreElements()) {
                if (++entries > MAX_ENTRIES) { truncated = true; break; }
                ZipEntry entry = iterator.nextElement();
                String pathName = entry.getName();
                String lower = pathName.toLowerCase(Locale.ROOT);
                if (entry.isDirectory() || !pathName.startsWith("shaders/")
                        || !ShaderPackArchiveValidator.isSafeEntryName(pathName)
                        || (!lower.endsWith(".vsh") && !lower.endsWith(".fsh"))) continue;
                if (stages >= MAX_SOURCE_FILES) { truncated = true; break; }
                long size = entry.getSize();
                if (size < 0 || size > MAX_FILE_BYTES || size > MAX_TOTAL_BYTES - bytes) {
                    truncated = true; continue;
                }
                StageResult result;
                try (InputStream in = zip.getInputStream(entry)) { result = scan(in, size); }
                bytes += size;
                stages++;
                if (!result.hasMain()) missingMain++;
                if (result.version() != null) { versioned++; versions.add(result.version()); }
                includes += result.includes();
                extensions.addAll(result.extensions());
                extensionCount += result.extensionDirectives();
            }
        }
        return new Summary(stages, missingMain, versioned, versions, includes, extensions, extensionCount, truncated);
    }

    private static StageResult scan(InputStream input, long declaredSize) throws IOException {
        if (declaredSize > MAX_FILE_BYTES) throw new IOException("GLSL source exceeds limit");
        StringBuilder source = new StringBuilder((int) Math.min(declaredSize, MAX_FILE_BYTES));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (source.length() + read > MAX_FILE_BYTES) throw new IOException("GLSL source exceeds limit");
                source.append(buffer, 0, read);
            }
        }

        String withoutComments = stripComments(source);
        String code = maskQuotedText(withoutComments);
        Matcher main = MAIN.matcher(code);
        Matcher version = VERSION.matcher(withoutComments);
        String versionValue = version.find() ? version.group(1)
                + (version.group(2) == null ? "" : version.group(2)) : null;
        Matcher includeMatcher = INCLUDE.matcher(withoutComments);
        int includeCount = 0;
        while (includeMatcher.find()) includeCount++;
        Matcher extensionMatcher = EXTENSION.matcher(withoutComments);
        Set<String> extensions = new TreeSet<>();
        int extensionDirectiveCount = 0;
        while (extensionMatcher.find()) {
            extensions.add(extensionMatcher.group(1));
            extensionDirectiveCount++;
        }
        return new StageResult(main.find(), versionValue, includeCount, extensions,
                extensionDirectiveCount);
    }

    /** Removes line and block comments while preserving newlines and quoted strings. */
    private static String stripComments(CharSequence source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean lineComment = false, blockComment = false, quoted = false, escaped = false;
        char quote = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (lineComment) {
                if (c == '\n' || c == '\r') { lineComment = false; out.append(c); }
                else out.append(' ');
            } else if (blockComment) {
                if (c == '*' && next == '/') { out.append("  "); i++; blockComment = false; }
                else out.append(c == '\n' || c == '\r' ? c : ' ');
            } else if (quoted) {
                out.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quoted = false;
            } else if (c == '/' && next == '/') {
                out.append("  "); i++; lineComment = true;
            } else if (c == '/' && next == '*') {
                out.append("  "); i++; blockComment = true;
            } else {
                out.append(c);
                if (c == '"' || c == '\'') { quoted = true; quote = c; }
            }
        }
        return out.toString();
    }

    /** Masks string/character contents so keywords inside literals are not treated as GLSL code. */
    private static String maskQuotedText(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean quoted = false, escaped = false;
        char quote = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quoted) {
                if (c == '\n' || c == '\r') out.append(c); else out.append(' ');
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quoted = false;
            } else {
                if (c == '"' || c == '\'') { quoted = true; quote = c; out.append(' '); }
                else out.append(c);
            }
        }
        return out.toString();
    }
}
