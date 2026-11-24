package dev.opticore.client.shader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Pairs shader vertex and fragment files by relative path; does not compile GLSL. */
public final class ShaderProgramIndex {
    private static final int MAX_ENTRIES = 20_000;
    private static final int MAX_PROGRAMS = 4_096;
    private ShaderProgramIndex() {}

    public record Summary(int pairedPrograms, int vertexOnly, int fragmentOnly,
                          Set<String> paired, Set<String> unmatchedVertex,
                          Set<String> unmatchedFragment, boolean truncated) {
        public Summary {
            paired = Set.copyOf(paired);
            unmatchedVertex = Set.copyOf(unmatchedVertex);
            unmatchedFragment = Set.copyOf(unmatchedFragment);
        }
        public static Summary empty() {
            return new Summary(0, 0, 0, Set.of(), Set.of(), Set.of(), false);
        }
    }

    public static Summary inspect(Path path, boolean archive) {
        if (path == null) return Summary.empty();
        try { return archive ? inspectArchive(path) : inspectDirectory(path); }
        catch (IOException | SecurityException ignored) { return Summary.empty(); }
    }

    static Summary inspectArchive(Path path) throws IOException {
        Set<String> vertex = new TreeSet<>(), fragment = new TreeSet<>();
        boolean truncated = false;
        int count = 0;
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (++count > MAX_ENTRIES) { truncated = true; break; }
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !ShaderPackArchiveValidator.isSafeEntryName(name)
                        || !name.startsWith("shaders/")) continue;
                String id = programId(name.substring("shaders/".length()));
                if (id == null) continue;
                if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".vsh")) vertex.add(id);
                else fragment.add(id);
                if (vertex.size() + fragment.size() > MAX_PROGRAMS) { truncated = true; break; }
            }
        }
        return summarize(vertex, fragment, truncated);
    }

    static Summary inspectDirectory(Path directory) throws IOException {
        Path root = directory.resolve("shaders").normalize();
        if (!root.startsWith(directory.normalize()) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            return Summary.empty();
        Set<String> vertex = new TreeSet<>(), fragment = new TreeSet<>();
        boolean truncated = false;
        int count = 0;
        try (var paths = Files.walk(root, 32)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                if (++count > MAX_ENTRIES) { truncated = true; break; }
                Path file = iterator.next();
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                String relative = root.relativize(file).toString()
                        .replace(file.getFileSystem().getSeparator(), "/");
                String id = programId(relative);
                if (id == null) continue;
                if (relative.toLowerCase(java.util.Locale.ROOT).endsWith(".vsh")) vertex.add(id);
                else fragment.add(id);
                if (vertex.size() + fragment.size() > MAX_PROGRAMS) { truncated = true; break; }
            }
        }
        return summarize(vertex, fragment, truncated);
    }

    private static String programId(String relative) {
        String lower = relative.toLowerCase(java.util.Locale.ROOT);
        if (!lower.endsWith(".vsh") && !lower.endsWith(".fsh")) return null;
        int dot = relative.lastIndexOf('.');
        if (dot <= 0) return null;
        String id = relative.substring(0, dot);
        return id.isBlank() || id.startsWith("/") || id.contains("..") ? null : id;
    }

    private static Summary summarize(Set<String> vertex, Set<String> fragment, boolean truncated) {
        Set<String> paired = new TreeSet<>(vertex);
        paired.retainAll(fragment);
        Set<String> vertexOnly = new TreeSet<>(vertex);
        vertexOnly.removeAll(fragment);
        Set<String> fragmentOnly = new TreeSet<>(fragment);
        fragmentOnly.removeAll(vertex);
        return new Summary(paired.size(), vertexOnly.size(), fragmentOnly.size(),
                paired, vertexOnly, fragmentOnly, truncated);
    }
}
