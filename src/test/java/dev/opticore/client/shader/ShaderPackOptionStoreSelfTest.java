package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ShaderPackOptionStoreSelfTest {
    private ShaderPackOptionStoreSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("opticore-option-store-test");
        Path file = directory.resolve("shader-options.properties");
        check(ShaderPackOptionStore.save(file, "Example Pack", "shadowQuality", "0.75"),
                "save option");
        check(ShaderPackOptionStore.save(file, "Example Pack", "WATER_REFLECT", "true"),
                "save second option");
        check(ShaderPackOptionStore.save(file, "Other Pack", "shadowQuality", "0.25"),
                "save other pack");

        Map<String, String> values = ShaderPackOptionStore.load(file, "Example Pack");
        check("0.75".equals(values.get("shadowQuality")), "load first value");
        check("true".equals(values.get("WATER_REFLECT")), "load second value");
        check(values.size() == 2, "pack values isolated");
        check("0.25".equals(ShaderPackOptionStore.load(file, "Other Pack")
                .get("shadowQuality")), "other pack isolated");

        check(ShaderPackOptionStore.load(null, "Example Pack").isEmpty(),
                "null load path returns empty values");
        check(!ShaderPackOptionStore.save(null, "Example Pack", "option", "x"),
                "null save path is rejected");
        check(!ShaderPackOptionStore.save(file, "../escape", "option", "x"),
                "reject unsafe pack name");
        check(!ShaderPackOptionStore.save(file, "Example Pack", "bad/id", "x"),
                "reject invalid option identifier");
        check(!ShaderPackOptionStore.save(file, "Example Pack", "option", "x".repeat(257)),
                "reject oversized value");
        check(ShaderPackOptionStore.save(file, "Example Pack", "shadowQuality", null),
                "remove option override");
        check(!ShaderPackOptionStore.load(file, "Example Pack").containsKey("shadowQuality"),
                "removed option absent");

        Files.deleteIfExists(file);
        Files.deleteIfExists(directory);
        System.out.println("ShaderPackOptionStoreSelfTest: 14 checks passed");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
