package dev.opticore.client.shader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Bounded persistent storage for future shader-pack option widgets.
 * Values are stored per pack and option identifier; this class does not
 * interpret values or apply them to a shader program.
 */
public final class ShaderPackOptionStore {
    private static final String PREFIX = "option.";
    private static final int MAX_FILE_BYTES = 512 * 1024;
    private static final int MAX_ENTRIES = 512;
    private static final int MAX_VALUE_LENGTH = 256;

    private ShaderPackOptionStore() {}

    public static synchronized Map<String, String> load(Path file, String packName) {
        if (!validPackName(packName)) return Map.of();
        Properties properties = read(file);
        String prefix = PREFIX + encode(packName) + ".";
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(prefix)) continue;
            String option = decode(key.substring(prefix.length()));
            String value = properties.getProperty(key);
            if (validIdentifier(option) && validValue(value)) {
                values.put(option, value);
            }
        }
        return Map.copyOf(values);
    }

    public static synchronized boolean save(Path file, String packName,
                                            String optionId, String value) {
        if (!validPackName(packName) || !validIdentifier(optionId)
                || (value != null && !validValue(value))) return false;

        Properties properties = read(file);
        String key = PREFIX + encode(packName) + "." + encode(optionId);
        if (value == null) properties.remove(key);
        else properties.setProperty(key, value);

        if (properties.size() > MAX_ENTRIES) return false;
        Path parent = file.toAbsolutePath().normalize().getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "opticore-options-", ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "OptiCore shader pack option values");
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

    private static boolean validPackName(String value) {
        return value != null && !value.isBlank() && value.length() <= 128
                && !value.contains("/") && !value.contains("\\") && !value.equals(".")
                && !value.equals("..");
    }

    private static boolean validIdentifier(String value) {
        return value != null && value.length() <= 128
                && value.matches("[A-Za-z0-9_.-]+");
    }

    private static boolean validValue(String value) {
        return value != null && value.length() <= MAX_VALUE_LENGTH
                && value.indexOf('\0') < 0;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }
}
