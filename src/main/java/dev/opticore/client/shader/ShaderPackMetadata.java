package dev.opticore.client.shader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads the optional OptiFine-style shader pack properties file.
 * This inspects declarations only; it does not compile or activate shaders.
 */
public final class ShaderPackMetadata {
    private static final String PROPERTIES_PATH = "shaders/shaders.properties";
    private static final int MAX_PROPERTIES_BYTES = ShaderPropertiesParser.MAX_PROPERTIES_BYTES;

    private ShaderPackMetadata() {}

    public record Summary(boolean hasProperties, int propertyCount, int sliderCount,
                          String screenDefinition, List<String> controls) {
        public Summary {
            controls = List.copyOf(controls);
        }

        public static Summary empty() {
            return new Summary(false, 0, 0, "", List.of());
        }
    }

    public static Summary inspect(ShaderPackRepository.Pack pack) {
        try (InputStream input = openProperties(pack)) {
            if (input == null) return Summary.empty();
            byte[] bytes = input.readNBytes(MAX_PROPERTIES_BYTES + 1);
            if (bytes.length > MAX_PROPERTIES_BYTES) return Summary.empty();

            return parseProperties(bytes);
        } catch (IOException | IllegalArgumentException ignored) {
            return Summary.empty();
        }
    }


    static Summary parseProperties(byte[] bytes) {
        ShaderPropertiesParser.Summary parsed = ShaderPropertiesParser.parse(bytes);
        return new Summary(parsed.hasProperties(), parsed.propertyCount(), parsed.sliderCount(),
                parsed.screenDefinition(), parsed.controls());
    }

    private static InputStream openProperties(ShaderPackRepository.Pack pack) throws IOException {
        if (pack.archive()) {
            ZipFile zip = new ZipFile(pack.path().toFile());
            ZipEntry entry = zip.getEntry(PROPERTIES_PATH);
            if (entry == null || entry.isDirectory() || entry.getSize() > MAX_PROPERTIES_BYTES) {
                zip.close();
                return null;
            }
            return new java.io.FilterInputStream(zip.getInputStream(entry)) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { zip.close(); }
                }
            };
        }

        var file = pack.path().resolve(PROPERTIES_PATH).normalize();
        if (!file.startsWith(pack.path().normalize()) || !Files.isRegularFile(file)
                || Files.size(file) > MAX_PROPERTIES_BYTES) return null;
        return Files.newInputStream(file);
    }
}
