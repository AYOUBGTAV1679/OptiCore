package dev.opticore.client.shader;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Discovers shader packs without loading or compiling them.
 * A pack is considered discoverable when it contains shaders/ at its root.
 */
public final class ShaderPackRepository {
    private static final String ACTIVE_KEY = "activePack";
    private static final Path CONFIG_FILE = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("opticore").resolve("shaders.properties");

    private ShaderPackRepository() {}

    public record Pack(String name, Path path, boolean archive) {}

    public static List<Pack> discover() {
        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks");
        try {
            Files.createDirectories(directory);
            List<Pack> packs = new ArrayList<>();
            try (var entries = Files.list(directory)) {
                entries.filter(Files::isDirectory).forEach(path -> {
                    if (Files.isDirectory(path.resolve("shaders"))) {
                        packs.add(new Pack(path.getFileName().toString(), path, false));
                    }
                });
                entries = null;
            } catch (UnsupportedOperationException ignored) {
                // Directory listing is handled by the outer IOException guard.
            }
            try (var entries = Files.list(directory)) {
                entries.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                        .forEach(path -> {
                            if (containsShaderDirectory(path)) {
                                packs.add(new Pack(path.getFileName().toString(), path, true));
                            }
                        });
            }
            packs.sort(Comparator.comparing(Pack::name, String.CASE_INSENSITIVE_ORDER));
            return packs;
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static boolean containsShaderDirectory(Path archive) {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            return zip.stream().map(ZipEntry::getName)
                    .anyMatch(name -> name.startsWith("shaders/") && !name.startsWith("../")
                            && !name.startsWith("/") && !name.contains("/../"));
        } catch (IOException ignored) {
            return false;
        }
    }

    public static String activePack() {
        Properties properties = readProperties();
        return properties.getProperty(ACTIVE_KEY, "OFF");
    }

    public static boolean select(String name) {
        Properties properties = readProperties();
        properties.setProperty(ACTIVE_KEY, name);
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (var writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                properties.store(writer, "OptiCore shader selection");
            }
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static Properties readProperties() {
        Properties properties = new Properties();
        if (Files.isRegularFile(CONFIG_FILE)) {
            try (var reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException ignored) {
                // Keep defaults when the local selection file is unreadable.
            }
        }
        return properties;
    }
}
