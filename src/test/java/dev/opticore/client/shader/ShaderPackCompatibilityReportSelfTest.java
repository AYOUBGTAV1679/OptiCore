package dev.opticore.client.shader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class ShaderPackCompatibilityReportSelfTest {
    private ShaderPackCompatibilityReportSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path pack = Files.createTempDirectory("opticore-compatibility");
        try {
            write(pack.resolve("shaders/world.vsh"),
                    "#version 150\nout vec2 TexCoord;\nvoid main() {}\n");
            write(pack.resolve("shaders/world.fsh"),
                    "#version 150\nin vec2 TexCoord;\nout vec4 fragColor;\nvoid main() {}\n");
            write(pack.resolve("shaders/broken.vsh"),
                    "#version 150\nout vec3 Varying;\nvoid main() {}\n");
            write(pack.resolve("shaders/broken.fsh"),
                    "#version 150\nin vec2 Varying;\nout vec4 fragColor;\nvoid main() {}\n");
            write(pack.resolve("shaders/vertex_only.vsh"),
                    "#version 150\nvoid main() {}\n");

            var empty = ShaderPackCompatibilityReport.inspect(null, false);
            check(!empty.allInspectedProgramsReady(), "empty inventory is not ready");

            var report = ShaderPackCompatibilityReport.inspect(pack, false);
            check(report.pairedPrograms() == 2, "paired programs counted");
            check(report.inspectedPrograms() == 2, "paired programs inspected");
            check(report.readyPrograms() == 1, "compatible pair accepted");
            check(report.blockedPrograms() == 1, "interface mismatch blocked");
            check(report.vertexOnlyPrograms() == 1, "unmatched vertex reported");
            check(!report.allInspectedProgramsReady(), "incomplete pack not declared ready");
            check(report.programs().stream().anyMatch(p -> p.id().equals("broken")
                    && !p.diagnostics().isEmpty()), "blocked program has diagnostics");
            System.out.println("ShaderPackCompatibilityReportSelfTest: 8 checks passed");
        } finally {
            try (var paths = Files.walk(pack)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                });
            }
        }
    }

    private static void write(Path path, String source) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, source, StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
