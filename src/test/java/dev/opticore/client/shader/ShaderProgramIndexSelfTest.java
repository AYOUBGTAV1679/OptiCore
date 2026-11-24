package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShaderProgramIndexSelfTest {
    private ShaderProgramIndexSelfTest() {}
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("opticore-programs");
        Path shaders = Files.createDirectories(folder.resolve("shaders/gbuffers"));
        Files.writeString(shaders.resolve("terrain.vsh"), "v");
        Files.writeString(shaders.resolve("terrain.fsh"), "f");
        Files.writeString(shaders.resolve("water.vsh"), "v");
        Files.writeString(folder.resolve("shaders/composite.fsh"), "f");
        Files.writeString(folder.resolve("shaders/not-a-program.glsl"), "i");
        ShaderProgramIndex.Summary dir = ShaderProgramIndex.inspectDirectory(folder);
        check(dir.pairedPrograms() == 1, "directory paired program");
        check(dir.unmatchedVertex().contains("gbuffers/water"), "directory vertex-only");
        check(dir.unmatchedFragment().contains("composite"), "directory fragment-only");

        Path zipPath = Files.createTempFile("opticore-programs", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            add(zip, "shaders/gbuffers/terrain.vsh");
            add(zip, "shaders/gbuffers/terrain.fsh");
            add(zip, "shaders/shadow.vsh");
            add(zip, "shaders/other.txt");
            add(zip, "../shaders/unsafe.fsh");
        }
        ShaderProgramIndex.Summary archive = ShaderProgramIndex.inspectArchive(zipPath);
        check(archive.pairedPrograms() == 1, "archive paired program");
        check(archive.unmatchedVertex().contains("shadow"), "archive vertex-only");
        check(!archive.unmatchedFragment().contains("unsafe"), "unsafe path ignored");

        Files.deleteIfExists(shaders.resolve("terrain.vsh"));
        Files.deleteIfExists(shaders.resolve("terrain.fsh"));
        Files.deleteIfExists(shaders.resolve("water.vsh"));
        Files.deleteIfExists(folder.resolve("shaders/composite.fsh"));
        Files.deleteIfExists(folder.resolve("shaders/not-a-program.glsl"));
        Files.deleteIfExists(shaders);
        Files.deleteIfExists(folder.resolve("shaders"));
        Files.deleteIfExists(folder);
        Files.deleteIfExists(zipPath);
        System.out.println("ShaderProgramIndexSelfTest: 6 checks passed");
    }
    private static void add(ZipOutputStream zip, String name) throws Exception {
        zip.putNextEntry(new ZipEntry(name)); zip.write(1); zip.closeEntry();
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
