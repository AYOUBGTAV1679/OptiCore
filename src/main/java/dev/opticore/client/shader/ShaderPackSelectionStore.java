package dev.opticore.client.shader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Bounded, crash-resistant persistence for the selected shader pack. */
public final class ShaderPackSelectionStore {
    private static final String ACTIVE_KEY = "activePack";
    private static final int MAX_FILE_BYTES = 32 * 1024;
    private ShaderPackSelectionStore() {}

    public static synchronized String load(Path file) {
        if (file == null) return "OFF";
        Properties properties = read(file);
        String selected = properties.getProperty(ACTIVE_KEY, "OFF");
        return validSelection(selected) ? selected : "OFF";
    }

    public static synchronized boolean save(Path file, String packName) {
        if (file == null || !validSelection(packName)) return false;
        Properties properties = read(file);
        properties.setProperty(ACTIVE_KEY, packName);
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent == null) return false;
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "opticore-selection-", ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "OptiCore shader selection");
            }
            if (Files.size(temporary) > MAX_FILE_BYTES) {
                Files.deleteIfExists(temporary);
                return false;
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | SecurityException exception) {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
            return false;
        }
    }

    private static Properties read(Path file) {
        Properties properties = new Properties();
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) return properties;
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            }
        } catch (IOException | IllegalArgumentException | SecurityException ignored) {
            properties.clear();
        }
        return properties;
    }

    private static boolean validSelection(String value) {
        return "OFF".equals(value) || (value != null && !value.isBlank()
                && value.length() <= 128 && !value.contains("/") && !value.contains("\\")
                && !value.equals(".") && !value.equals("..") && value.indexOf('\0') < 0);
    }
}
