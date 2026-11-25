package dev.opticore.client.shader;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Discovers shader packs without loading or compiling them. */
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
            }
            try (var entries = Files.list(directory)) {
                entries.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
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
            return ShaderPackArchiveValidator.containsRootShaderDirectory(zip);
        } catch (IOException ignored) {
            return false;
        }
    }

    public static String activePack() {
        return readProperties().getProperty(ACTIVE_KEY, "OFF");
    }

    public static boolean select(String name) {
        if (!"OFF".equals(name) && discover().stream().noneMatch(pack -> pack.name().equals(name))) {
            return false;
        }
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

    /** Returns the saved, per-pack option overrides consumed by ShaderPackRuntime before compilation. */
    public static java.util.Map<String, String> optionOverrides(String packName) {
        Path optionFile = CONFIG_FILE.getParent().resolve("shader-pack-options.properties");
        return ShaderPackOptionStore.load(optionFile, packName);
    }

    /** Persists a validated option override for the selected pack's next compilation. */
    public static boolean saveOptionOverride(String packName, String optionId, String value) {
        Path optionFile = CONFIG_FILE.getParent().resolve("shader-pack-options.properties");
        return ShaderPackOptionStore.save(optionFile, packName, optionId, value);
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
