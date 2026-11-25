package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShaderOptionDiscoverySelfTest {
    private ShaderOptionDiscoverySelfTest() {}

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("opticore-options");
        Path shaders = Files.createDirectories(folder.resolve("shaders"));
        Files.writeString(shaders.resolve("composite.fsh"),
                "#define BLOOM // Bloom\n#ifdef BLOOM\n#endif\n#define QUALITY 2 // [0 1 2 3]");
        Files.writeString(shaders.resolve("common.glsl"),
                "// #define OLD_LIGHT // Legacy\n#ifndef OLD_LIGHT\n#endif");
        Files.writeString(shaders.resolve("ignored.txt"), "#define IGNORE 1");
        Files.writeString(shaders.resolve("shaders.properties"), "sliders=QUALITY,BLOOM\nscreen=QUALITY BLOOM");
        var directory = ShaderOptionDiscovery.inspectPath(folder, false);
        check(directory.options().containsKey("BLOOM"), "directory macro discovered");
        check(directory.options().containsKey("QUALITY"), "directory numeric option discovered");
        check(directory.options().containsKey("OLD_LIGHT"), "directory include option discovered");
        check(!directory.options().containsKey("IGNORE"), "non-shader files ignored");
        check(directory.options().get("QUALITY").slider(), "folder sliders metadata marks discovered option");

        Path archive = Files.createTempFile("opticore-options", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            add(zip, "shaders/composite.fsh", "#define SHADOWS // Shadows\n#ifdef SHADOWS\n#endif");
            add(zip, "shaders/shaders.properties", "sliders=SHADOWS\nscreen=SHADOWS");
            add(zip, "other/composite.fsh", "#define OUTSIDE 1");
            add(zip, "../shaders/traversal.fsh", "#define TRAVERSAL 1");
        }
        var zipped = ShaderOptionDiscovery.inspectPath(archive, true);
        check(zipped.options().containsKey("SHADOWS"), "archive shader source discovered");
        check(!zipped.options().containsKey("OUTSIDE"), "archive files outside shader root ignored");
        check(zipped.options().get("SHADOWS").slider(), "archive sliders metadata marks discovered option");
        check(!zipped.options().containsKey("TRAVERSAL"), "unsafe archive path ignored");

        Path cappedFolder = Files.createTempDirectory("opticore-options-capped");
        Path cappedShaders = Files.createDirectories(cappedFolder.resolve("shaders"));
        for (int i = 0; i < ShaderOptionParser.MAX_FILES + 1; i++) {
            Files.writeString(cappedShaders.resolve(String.format(java.util.Locale.ROOT,
                    "extra-%03d.glsl", i)), "#define OPTION_" + i + " 1");
        }
        var capped = ShaderOptionDiscovery.inspectPath(cappedFolder, false);
        check(capped.truncated(), "source inventory reports file-count cap");
        check(capped.scannedFiles() == ShaderOptionParser.MAX_FILES,
                "source inventory respects maximum source file count");

        Path oversizedFolder = Files.createTempDirectory("opticore-options-oversized");
        Path oversizedShaders = Files.createDirectories(oversizedFolder.resolve("shaders"));
        Files.writeString(oversizedShaders.resolve("oversized.glsl"),
                " ".repeat(ShaderOptionParser.MAX_FILE_CHARS + 1));
        var oversized = ShaderOptionDiscovery.inspectPath(oversizedFolder, false);
        check(oversized.truncated(), "source inventory reports oversized shader source");

        try (var files = Files.list(cappedShaders)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
        }
        Files.deleteIfExists(cappedShaders);
        Files.deleteIfExists(cappedFolder);
        Files.deleteIfExists(oversizedShaders.resolve("oversized.glsl"));
        Files.deleteIfExists(oversizedShaders);
        Files.deleteIfExists(oversizedFolder);

        Files.deleteIfExists(shaders.resolve("composite.fsh"));
        Files.deleteIfExists(shaders.resolve("common.glsl"));
        Files.deleteIfExists(shaders.resolve("ignored.txt"));
        Files.deleteIfExists(shaders.resolve("shaders.properties"));
        Files.deleteIfExists(shaders);
        Files.deleteIfExists(folder);
        Files.deleteIfExists(archive);
        System.out.println("ShaderOptionDiscoverySelfTest: 12 checks passed");
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
