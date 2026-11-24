package dev.opticore.client.shader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bounded inventory of shader source files. It never extracts archives or reads
 * shader source contents, and therefore does not claim that a pack can compile.
 */
public final class ShaderPackInventory {
    public static final int MAX_ARCHIVE_ENTRIES = 20_000;
    public static final int MAX_SOURCE_FILES = 4_096;
    public static final long MAX_DECLARED_SOURCE_BYTES = 64L * 1024 * 1024;
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(".vsh", ".fsh", ".glsl");

    private ShaderPackInventory() {}

    public record Summary(int sourceFiles, int vertexFiles, int fragmentFiles,
                          int includeFiles, long declaredBytes, List<String> programs,
                          boolean truncated) {
        public Summary {
            programs = List.copyOf(programs);
        }

        public static Summary empty() {
            return new Summary(0, 0, 0, 0, 0L, List.of(), false);
        }
    }

    public static Summary inspect(ShaderPackRepository.Pack pack) {
        if (pack == null || pack.path() == null) return Summary.empty();
        try {
            return pack.archive() ? inspectArchive(pack.path()) : inspectDirectory(pack.path());
        } catch (IOException | SecurityException ignored) {
            return Summary.empty();
        }
    }

    static Summary inspectArchive(Path archivePath) throws IOException {
        int totalEntries = 0;
        int sourceFiles = 0;
        int vertexFiles = 0;
        int fragmentFiles = 0;
        int includeFiles = 0;
        long declaredBytes = 0;
        boolean truncated = false;
        Set<String> programs = new TreeSet<>();
        try (ZipFile archive = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                if (++totalEntries > MAX_ARCHIVE_ENTRIES) {
                    truncated = true;
                    break;
                }
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !isShaderSource(name)) continue;
                if (!ShaderPackArchiveValidator.isSafeEntryName(name)) continue;
                if (!name.startsWith("shaders/")) continue;
                if (sourceFiles >= MAX_SOURCE_FILES) {
                    truncated = true;
                    break;
                }
                long size = entry.getSize();
                if (size < 0 || size > MAX_DECLARED_SOURCE_BYTES - declaredBytes) {
                    truncated = true;
                    continue;
                }
                declaredBytes += size;
                sourceFiles++;
                if (name.endsWith(".vsh")) vertexFiles++;
                else if (name.endsWith(".fsh")) fragmentFiles++;
                else includeFiles++;
                addProgram(programs, name);
            }
        }
        return new Summary(sourceFiles, vertexFiles, fragmentFiles, includeFiles,
                declaredBytes, new ArrayList<>(programs), truncated);
    }

    static Summary inspectDirectory(Path packDirectory) throws IOException {
        Path root = packDirectory.resolve("shaders").normalize();
        if (!root.startsWith(packDirectory.normalize()) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return Summary.empty();
        }
        int sourceFiles = 0;
        int vertexFiles = 0;
        int fragmentFiles = 0;
        int includeFiles = 0;
        long declaredBytes = 0;
        boolean truncated = false;
        Set<String> programs = new TreeSet<>();
        try (var paths = Files.walk(root, 32)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                String relative = root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
                if (!isShaderSource(relative)) continue;
                if (sourceFiles >= MAX_SOURCE_FILES) {
                    truncated = true;
                    break;
                }
                long size = Files.size(file);
                if (size > MAX_DECLARED_SOURCE_BYTES - declaredBytes) {
                    truncated = true;
                    continue;
                }
                declaredBytes += size;
                sourceFiles++;
                String lower = relative.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".vsh")) vertexFiles++;
                else if (lower.endsWith(".fsh")) fragmentFiles++;
                else includeFiles++;
                addProgram(programs, relative);
            }
        }
        return new Summary(sourceFiles, vertexFiles, fragmentFiles, includeFiles,
                declaredBytes, new ArrayList<>(programs), truncated);
    }

    private static boolean isShaderSource(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return SOURCE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private static void addProgram(Set<String> programs, String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        int dot = file.lastIndexOf('.');
        if (dot > 0) programs.add(file.substring(0, dot));
    }
}
