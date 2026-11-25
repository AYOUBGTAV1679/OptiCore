package dev.opticore.client.shader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bounded include expander for shader sources. It deliberately accepts only
 * safe paths inside the pack's shaders directory and never extracts archives.
 */
public final class ShaderIncludeResolver {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_FILES = 256;
    private static final int MAX_SOURCE_BYTES = 512 * 1024;
    private static final int MAX_TOTAL_BYTES = 4 * 1024 * 1024;
    private static final Pattern INCLUDE = Pattern.compile(
            "^[ \\t]*#[ \\t]*include[ \\t]+[<\\\"]([^>\\\"]+)[>\\\"][ \\t]*(?://.*)?$");
    private ShaderIncludeResolver() {}

    public record Result(String source, List<String> includedFiles, List<String> errors,
                         boolean truncated) {
        public Result {
            includedFiles = List.copyOf(includedFiles);
            errors = List.copyOf(errors);
        }
        public boolean successful() { return errors.isEmpty() && !truncated; }
    }

    public static Result resolve(Path pack, boolean archive, String entryName) {
        if (pack == null || !safePackPath(entryName))
            return new Result("", List.of(), List.of("Invalid shader entry path"), false);
        try {
            Resolver resolver = new Resolver(pack, archive);
            String expanded = resolver.expand(entryName, 0);
            return new Result(expanded, resolver.included, resolver.errors, resolver.truncated);
        } catch (IOException | SecurityException ex) {
            return new Result("", List.of(), List.of("Unable to read shader source"), false);
        }
    }

    private static boolean safePackPath(String name) {
        return name != null && name.startsWith("shaders/") && ShaderPackArchiveValidator.isSafeEntryName(name);
    }

    private static final class Resolver {
        private final Path pack;
        private final boolean archive;
        private final Deque<String> stack = new ArrayDeque<>();
        private final Set<String> seen = new HashSet<>();
        private final List<String> included = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private int totalBytes;
        private boolean truncated;

        private Resolver(Path pack, boolean archive) {
            this.pack = pack.toAbsolutePath().normalize();
            this.archive = archive;
        }

        private String expand(String name, int depth) throws IOException {
            if (depth > MAX_DEPTH) { fail("Include depth limit exceeded"); return ""; }
            if (!safePackPath(name)) { fail("Unsafe include path: " + name); return ""; }
            if (stack.contains(name)) { fail("Include cycle: " + name); return ""; }
            if (!seen.contains(name) && seen.size() >= MAX_FILES) {
                truncated = true; fail("Include file limit exceeded"); return "";
            }
            byte[] bytes = read(name);
            if (bytes == null) { fail("Missing include: " + name); return ""; }
            if (bytes.length > MAX_SOURCE_BYTES || bytes.length > MAX_TOTAL_BYTES - totalBytes) {
                truncated = true; fail("Shader source byte limit exceeded"); return "";
            }
            totalBytes += bytes.length;
            seen.add(name);
            if (depth > 0 && !included.contains(name)) included.add(name);
            String source = new String(bytes, StandardCharsets.UTF_8);
            StringBuilder output = new StringBuilder(source.length());
            stack.push(name);
            try {
                String[] lines = source.split("(?<=\\n)", -1);
                for (String line : lines) {
                    String body = line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
                    // split() retains the carriage return in CRLF files; ignore it for directive matching.
                    if (body.endsWith("\r")) body = body.substring(0, body.length() - 1);
                    Matcher matcher = INCLUDE.matcher(body);
                    if (!matcher.matches()) { output.append(line); continue; }
                    String target = resolveName(name, matcher.group(1));
                    if (target == null) {
                        fail("Unsafe include reference in " + name + ": " + matcher.group(1));
                        output.append('\n');
                    } else {
                        output.append(expand(target, depth + 1));
                        if (!line.endsWith("\n") && output.length() > 0
                                && output.charAt(output.length() - 1) != '\n') output.append('\n');
                    }
                    if (truncated) break;
                }
            } finally {
                stack.pop();
            }
            return output.toString();
        }

        private byte[] read(String name) throws IOException {
            if (archive) {
                try (ZipFile zip = new ZipFile(pack.toFile())) {
                    ZipEntry entry = zip.getEntry(name);
                    if (entry == null || entry.isDirectory() || !ShaderPackArchiveValidator.isSafeEntryName(entry.getName())
                            || entry.getSize() < 0 || entry.getSize() > MAX_SOURCE_BYTES) return null;
                    try (var in = zip.getInputStream(entry)) { return readBounded(in, MAX_SOURCE_BYTES); }
                }
            }
            Path root = pack.resolve("shaders").normalize();
            Path target = pack.resolve(name).normalize();
            if (!target.startsWith(root) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) return null;
            Path current = root;
            Path relative = root.relativize(target);
            for (Path part : relative) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)) return null;
            }
            long size = Files.size(target);
            if (size > MAX_SOURCE_BYTES) return null;
            try (var in = Files.newInputStream(target)) { return readBounded(in, MAX_SOURCE_BYTES); }
        }

        private static byte[] readBounded(java.io.InputStream in, int max) throws IOException {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > max) throw new IOException("Source too large");
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }

        private static String resolveName(String including, String requested) {
            if (requested.isBlank() || requested.startsWith("/") || requested.indexOf('\\') >= 0
                    || requested.indexOf('\0') >= 0 || requested.matches("^[A-Za-z]:.*")) return null;
            // Shader packs commonly use both paths relative to the including file
            // and paths rooted at the pack's shaders/ directory.
            if (requested.startsWith("shaders/")) {
                return safePackPath(requested) ? requested : null;
            }
            String parent = including.substring(0, including.lastIndexOf('/') + 1);
            String relative = parent + requested;
            if (!safePackPath(relative)) relative = "shaders/" + requested;
            return safePackPath(relative) ? relative : null;
        }

        private void fail(String message) { errors.add(message); }
    }
}
