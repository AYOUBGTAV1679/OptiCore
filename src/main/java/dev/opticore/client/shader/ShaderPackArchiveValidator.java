package dev.opticore.client.shader;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Applies conservative entry-count and path checks before a shader archive is listed. */
public final class ShaderPackArchiveValidator {
    static final int MAX_ENTRIES = 20_000;
    static final int MAX_ENTRY_NAME_LENGTH = 1_024;

    private ShaderPackArchiveValidator() {}

    public static boolean containsRootShaderDirectory(ZipFile archive) throws IOException {
        Enumeration<? extends ZipEntry> entries = archive.entries();
        int count = 0;
        boolean foundRootShaderFile = false;
        while (entries.hasMoreElements()) {
            if (++count > MAX_ENTRIES) return false;
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!isSafeEntryName(name)) continue;
            if (name.startsWith("shaders/") && name.length() > "shaders/".length()) {
                foundRootShaderFile = true;
            }
        }
        return foundRootShaderFile;
    }

    static boolean isSafeEntryName(String name) {
        if (name == null || name.isEmpty() || name.length() > MAX_ENTRY_NAME_LENGTH
                || name.startsWith("/") || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) return false;
        if (name.matches("^[A-Za-z]:.*")) return false;
        for (String segment : name.split("/")) {
            if (segment.equals("..") || segment.equals(".")) return false;
        }
        return true;
    }
}
