package dev.opticore.client.shader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Bounded source reader feeding shader declarations to ShaderOptionParser. */
public final class ShaderOptionDiscovery {
    public static final int MAX_TOTAL_SOURCE_BYTES = 4 * 1024 * 1024;
    private static final String ROOT = "shaders/";

    private ShaderOptionDiscovery() {}

    public static ShaderOptionParser.Summary inspect(Path path, boolean archive) {
        if (path == null) return ShaderOptionParser.Summary.empty();
        try {
            return inspectPath(path, archive);
        } catch (IOException | SecurityException ignored) {
            return ShaderOptionParser.Summary.empty();
        }
    }

    static ShaderOptionParser.Summary inspectPath(Path path, boolean archive) throws IOException {
        SourceRead read = archive ? readArchive(path) : readDirectory(path);
        ShaderOptionParser.Summary parsed = ShaderOptionParser.parse(
                read.sources(), readSliderIds(path, archive));
        return new ShaderOptionParser.Summary(parsed.options(),
                parsed.truncated() || read.truncated(), parsed.scannedFiles());
    }

    private record SourceRead(List<ShaderOptionParser.Source> sources, boolean truncated) {
        private SourceRead {
            sources = List.copyOf(sources);
        }
    }

    private static Set<String> readSliderIds(Path pack, boolean archive) {
        try {
            byte[] bytes;
            if (archive) {
                try (ZipFile zip = new ZipFile(pack.toFile())) {
                    ZipEntry entry = zip.getEntry("shaders/shaders.properties");
                    if (entry == null || entry.isDirectory()
                            || entry.getSize() < 0
                            || entry.getSize() > ShaderPropertiesParser.MAX_PROPERTIES_BYTES) return Set.of();
                    try (InputStream input = zip.getInputStream(entry)) {
                        bytes = input.readNBytes(ShaderPropertiesParser.MAX_PROPERTIES_BYTES + 1);
                    }
                }
            } else {
                Path file = pack.resolve("shaders/shaders.properties").normalize();
                if (!file.startsWith(pack.normalize()) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        || Files.size(file) > ShaderPropertiesParser.MAX_PROPERTIES_BYTES) return Set.of();
                try (InputStream input = Files.newInputStream(file)) {
                    bytes = input.readNBytes(ShaderPropertiesParser.MAX_PROPERTIES_BYTES + 1);
                }
            }
            return ShaderPropertiesParser.parseSliderIds(bytes);
        } catch (IOException | SecurityException ignored) {
            return Set.of();
        }
    }

    private static SourceRead readDirectory(Path pack) throws IOException {
        Path root = pack.resolve(ROOT).normalize();
        if (!root.startsWith(pack.normalize())
                || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return new SourceRead(List.of(), false);
        }
        List<ShaderOptionParser.Source> sources = new ArrayList<>();
        int scanned = 0;
        int totalBytes = 0;
        boolean truncated = false;
        try (var paths = Files.walk(root, 32)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                if (++scanned > ShaderPackInventory.MAX_ARCHIVE_ENTRIES) {
                    truncated = true;
                    break;
                }
                Path file = iterator.next();
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        || !isSource(file.getFileName().toString())) continue;
                if (sources.size() >= ShaderOptionParser.MAX_FILES) {
                    truncated = true;
                    break;
                }
                long size = Files.size(file);
                if (size < 0 || size > ShaderOptionParser.MAX_FILE_CHARS
                        || size > MAX_TOTAL_SOURCE_BYTES - totalBytes) {
                    truncated = true;
                    continue;
                }
                byte[] bytes;
                try (InputStream input = Files.newInputStream(file)) {
                    bytes = input.readNBytes(ShaderOptionParser.MAX_FILE_CHARS + 1);
                }
                if (bytes.length > ShaderOptionParser.MAX_FILE_CHARS
                        || bytes.length > MAX_TOTAL_SOURCE_BYTES - totalBytes) {
                    truncated = true;
                    continue;
                }
                totalBytes += bytes.length;
                String relative = root.relativize(file).toString()
                        .replace(file.getFileSystem().getSeparator(), "/");
                sources.add(new ShaderOptionParser.Source(relative,
                        new String(bytes, java.nio.charset.StandardCharsets.UTF_8)));
            }
        }
        return new SourceRead(sources, truncated);
    }

    private static SourceRead readArchive(Path archivePath) throws IOException {
        List<ShaderOptionParser.Source> sources = new ArrayList<>();
        int totalEntries = 0;
        int totalBytes = 0;
        boolean truncated = false;
        try (ZipFile zip = new ZipFile(archivePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (++totalEntries > ShaderPackInventory.MAX_ARCHIVE_ENTRIES) {
                    truncated = true;
                    break;
                }
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(ROOT)
                        || !ShaderPackArchiveValidator.isSafeEntryName(name)
                        || !isSource(name)) continue;
                if (sources.size() >= ShaderOptionParser.MAX_FILES) {
                    truncated = true;
                    break;
                }
                long declared = entry.getSize();
                if (declared < 0 || declared > ShaderOptionParser.MAX_FILE_CHARS
                        || declared > MAX_TOTAL_SOURCE_BYTES - totalBytes) {
                    truncated = true;
                    continue;
                }
                byte[] bytes;
                try (InputStream input = zip.getInputStream(entry)) {
                    bytes = input.readNBytes(ShaderOptionParser.MAX_FILE_CHARS + 1);
                }
                if (bytes.length > ShaderOptionParser.MAX_FILE_CHARS
                        || bytes.length > MAX_TOTAL_SOURCE_BYTES - totalBytes) {
                    truncated = true;
                    continue;
                }
                totalBytes += bytes.length;
                sources.add(new ShaderOptionParser.Source(name.substring(ROOT.length()),
                        new String(bytes, java.nio.charset.StandardCharsets.UTF_8)));
            }
        }
        return new SourceRead(sources, truncated);
    }

    private static boolean isSource(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".vsh") || lower.endsWith(".fsh") || lower.endsWith(".glsl");
    }
}
