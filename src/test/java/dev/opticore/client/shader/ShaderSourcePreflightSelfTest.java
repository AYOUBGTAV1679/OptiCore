package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShaderSourcePreflightSelfTest {
    private ShaderSourcePreflightSelfTest() {}
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("opticore-glsl-preflight");
        Path shaders = Files.createDirectories(folder.resolve("shaders/gbuffers"));
        Files.writeString(shaders.resolve("terrain.vsh"),
                "#version 150\n#extension GL_ARB_explicit_attrib_location : enable\n"
                        + "#extension GL_ARB_explicit_attrib_location : warn\n"
                        + "// void main() {}\nvoid main() { gl_Position = vec4(0.0); }\n");
        Files.writeString(shaders.resolve("terrain.fsh"),
                "#version 150\n#include \"common.glsl\"\n"
                        + "/* void main() {} */\nvoid main() {}\n");
        Files.writeString(folder.resolve("shaders/shadow.fsh"),
                "#version 120\n// no entry point\n"
                        + "const char *fake = \"void main() { }\";\n");
        ShaderSourcePreflight.Summary dir = ShaderSourcePreflight.inspect(folder, false);
        check(dir.inspectedStages() == 3, "directory stages");
        check(dir.stagesWithoutMain() == 1, "comments and strings do not fake main");
        check(dir.versionedStages() == 3, "versions detected");
        check(dir.versions().contains("150") && dir.versions().contains("120"), "version set");
        check(dir.includeDirectives() == 1, "include count");
        check(dir.extensions().contains("GL_ARB_explicit_attrib_location")
                && dir.extensionDirectives() == 2, "unique extensions and total directive count");

        Path zipPath = Files.createTempFile("opticore-glsl-preflight", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            add(zip, "shaders/test.vsh", "/* void main() {} */\nvoid main() {}");
            add(zip, "shaders/test.fsh", "#version 150\nvoid main() {}");
            add(zip, "../shaders/evil.vsh", "void main() {}");
        }
        ShaderSourcePreflight.Summary archive = ShaderSourcePreflight.inspect(zipPath, true);
        check(archive.inspectedStages() == 2, "ZIP stages and unsafe path rejection");
        check(archive.stagesWithoutMain() == 0, "ZIP main detection");

        try (var paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        Files.deleteIfExists(zipPath);
        System.out.println("ShaderSourcePreflightSelfTest: 10 checks passed");
    }
    private static void add(ZipOutputStream zip, String name, String source) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
