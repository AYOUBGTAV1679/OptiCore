package dev.opticore.client.shader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShaderProgramDescriptorSelfTest {
    private ShaderProgramDescriptorSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("opticore-program-folder");
        Path zip = Files.createTempFile("opticore-program", ".zip");
        try {
            write(folder.resolve("shaders/lib/common.glsl"), "uniform float SharedValue;\n");
            write(folder.resolve("shaders/world.vsh"),
                    "#version 150\n#include \"lib/common.glsl\"\nout vec2 TexCoord;\nvoid main() {}\n");
            write(folder.resolve("shaders/world.fsh"),
                    "#version 150\nin vec2 TexCoord;\nout vec4 fragColor;\nvoid main() {}\n");

            var folderResult = ShaderProgramDescriptor.inspect(folder, false, "world");
            check(folderResult.readyForCompiler(), "folder pair assembled");
            check(folderResult.vertex().includedFiles().contains("shaders/lib/common.glsl"),
                    "vertex include recorded");
            check(folderResult.vertex().declarations().uniforms().size() == 1,
                    "include declaration inventoried");
            check(folderResult.interfaceCheck().matched().contains("TexCoord"),
                    "stage interface matched");

            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
                add(out, "shaders/world.vsh", "#version 150\nout vec2 TexCoord;\nvoid main() {}\n");
                add(out, "shaders/world.fsh", "#version 150\nin vec2 TexCoord;\nout vec4 fragColor;\nvoid main() {}\n");
            }
            var zipResult = ShaderProgramDescriptor.inspect(zip, true, "world");
            check(zipResult.readyForCompiler(), "zip pair assembled");

            var invalid = ShaderProgramDescriptor.inspect(folder, false, "../outside");
            check(!invalid.readyForCompiler(), "unsafe program id rejected");
            var missing = ShaderProgramDescriptor.inspect(folder, false, "missing");
            check(!missing.readyForCompiler() && !missing.diagnostics().isEmpty(),
                    "missing stage diagnosed");
            System.out.println("ShaderProgramDescriptorSelfTest: 5 checks passed");
        } finally {
            try (var paths = Files.walk(folder)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                });
            }
            Files.deleteIfExists(zip);
        }
    }

    private static void write(Path path, String source) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, source, StandardCharsets.UTF_8);
    }
    private static void add(ZipOutputStream out, String name, String source) throws Exception {
        out.putNextEntry(new ZipEntry(name));
        out.write(source.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
