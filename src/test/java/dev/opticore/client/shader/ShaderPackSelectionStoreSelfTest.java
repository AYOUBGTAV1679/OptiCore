package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ShaderPackSelectionStoreSelfTest {
    private ShaderPackSelectionStoreSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("opticore-selection-test");
        Path file = directory.resolve("shaders.properties");
        check("OFF".equals(ShaderPackSelectionStore.load(file)), "missing file defaults to OFF");
        check(ShaderPackSelectionStore.save(file, "Example Pack"), "save selected pack");
        check("Example Pack".equals(ShaderPackSelectionStore.load(file)), "load selected pack");
        check(ShaderPackSelectionStore.save(file, "Other Pack"), "replace selected pack");
        check("Other Pack".equals(ShaderPackSelectionStore.load(file)), "latest selection is loaded");
        check(ShaderPackSelectionStore.save(file, "OFF"), "OFF can be persisted");
        check("OFF".equals(ShaderPackSelectionStore.load(file)), "OFF selection is restored");
        check(!ShaderPackSelectionStore.save(file, "../escape"), "path traversal selection rejected");
        check(!ShaderPackSelectionStore.save(file, "bad/name"), "slash in selection rejected");
        check(!ShaderPackSelectionStore.save(file, "bad\\name"), "backslash in selection rejected");
        check(!ShaderPackSelectionStore.save(file, "x".repeat(129)), "oversized selection rejected");
        check(!ShaderPackSelectionStore.save(null, "Example Pack"), "null path rejected");
        check("OFF".equals(ShaderPackSelectionStore.load(null)), "null path safely defaults");
        Files.writeString(file, "activePack=../escape\n");
        check("OFF".equals(ShaderPackSelectionStore.load(file)), "unsafe persisted value falls back to OFF");
        Files.writeString(file, "activePack=Broken\n".repeat(4000));
        check("OFF".equals(ShaderPackSelectionStore.load(file)), "oversized file falls back to OFF");
        Files.deleteIfExists(file);
        Files.deleteIfExists(directory);
        System.out.println("ShaderPackSelectionStoreSelfTest: 15 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
