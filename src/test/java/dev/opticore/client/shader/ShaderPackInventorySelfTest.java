package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShaderPackInventorySelfTest {
    private ShaderPackInventorySelfTest() {}

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("opticore-pack");
        Path shaders = Files.createDirectories(folder.resolve("shaders"));
        Files.writeString(shaders.resolve("gbuffers_terrain.vsh"), "vertex");
        Files.writeString(shaders.resolve("gbuffers_terrain.fsh"), "fragment");
        Files.writeString(shaders.resolve("common.glsl"), "include");
        Files.writeString(folder.resolve("ignored.txt"), "not shader source");

        ShaderPackInventory.Summary directory = ShaderPackInventory.inspectDirectory(folder);
        check(directory.sourceFiles() == 3, "folder source count");
        check(directory.vertexFiles() == 1, "vertex count");
        check(directory.fragmentFiles() == 1, "fragment count");
        check(directory.includeFiles() == 1, "include count");
        check(directory.programs().contains("gbuffers_terrain"), "program stem found");

        Path zipPath = Files.createTempFile("opticore-pack", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            add(zip, "shaders/gbuffers_water.vsh", "v");
            add(zip, "shaders/gbuffers_water.fsh", "f");
            add(zip, "assets/not_shader.txt", "x");
            add(zip, "../shaders/traversal.vsh", "bad");
        }
        ShaderPackInventory.Summary archive = ShaderPackInventory.inspectArchive(zipPath);
        check(archive.sourceFiles() == 2, "archive source count");
        check(archive.programs().contains("gbuffers_water"), "archive program stem found");
        check(!archive.programs().contains("traversal"), "unsafe archive path ignored");
        check(archive.declaredBytes() == 2, "archive declared bytes counted");

        Files.deleteIfExists(shaders.resolve("gbuffers_terrain.vsh"));
        Files.deleteIfExists(shaders.resolve("gbuffers_terrain.fsh"));
        Files.deleteIfExists(shaders.resolve("common.glsl"));
        Files.deleteIfExists(shaders);
        Files.deleteIfExists(folder.resolve("ignored.txt"));
        Files.deleteIfExists(folder);
        Files.deleteIfExists(zipPath);
        System.out.println("ShaderPackInventorySelfTest: 9 checks passed");
    }

    private static void add(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
