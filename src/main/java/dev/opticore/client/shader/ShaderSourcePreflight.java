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
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*#\\s*version\\s+([0-9]+)(?:\\s+([A-Za-z]+))?");
    private ShaderSourcePreflight() {}

    public record Summary(int inspectedStages, int stagesWithoutMain, int versionedStages,
                          Set<String> versions, int includeDirectives, boolean truncated) {
        public Summary { versions = Set.copyOf(versions); }
        public static Summary empty() { return new Summary(0, 0, 0, Set.of(), 0, false); }
    }

    private record StageResult(boolean hasMain, String version, int includes) {}

    public static Summary inspect(Path pack, boolean archive) {
        if (pack == null) return Summary.empty();
        try { return archive ? inspectZip(pack) : inspectFolder(pack); }
        catch (IOException | SecurityException ignored) { return Summary.empty(); }
    }

    private static Summary inspectFolder(Path pack) throws IOException {
        Path root = pack.resolve("shaders").normalize();
        if (!root.startsWith(pack.normalize()) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            return Summary.empty();
        int entries = 0, stages = 0, missingMain = 0, versioned = 0, includes = 0;
        long bytes = 0;
        boolean truncated = false;
        Set<String> versions = new TreeSet<>();
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
            }
        }
        return new Summary(stages, missingMain, versioned, versions, includes, truncated);
    }

    private static Summary inspectZip(Path path) throws IOException {
        int entries = 0, stages = 0, missingMain = 0, versioned = 0, includes = 0;
        long bytes = 0;
        boolean truncated = false;
        Set<String> versions = new TreeSet<>();
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
            }
        }
        return new Summary(stages, missingMain, versioned, versions, includes, truncated);
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
        String text = source.toString();
        var main = MAIN.matcher(text);
        var version = VERSION.matcher(text);
        String versionValue = version.find() ? version.group(1)
                + (version.group(2) == null ? "" : version.group(2)) : null;
        int includeCount = 0;
        var lines = text.split("\\R");
        for (String line : lines) {
            if (line.stripLeading().startsWith("#include")) includeCount++;
        }
        return new StageResult(main.find(), versionValue, includeCount);
    }
}
