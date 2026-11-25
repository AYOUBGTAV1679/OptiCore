package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShaderIncludeResolverSelfTest {
    private ShaderIncludeResolverSelfTest() {}
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("opticore-include");
        Path shaders = Files.createDirectories(folder.resolve("shaders/lib"));
        Files.writeString(folder.resolve("shaders/main.fsh"),
                "#version 150\n#include \"lib/common.glsl\"\nvoid main() {}\n");
        Files.writeString(shaders.resolve("common.glsl"), "vec3 helper() { return vec3(1.0); }\n");
        ShaderIncludeResolver.Result folderResult =
                ShaderIncludeResolver.resolve(folder, false, "shaders/main.fsh");
        check(folderResult.successful(), "folder include resolves");
        check(folderResult.source().contains("vec3 helper()"), "included source is expanded");
        check(folderResult.includedFiles().contains("shaders/lib/common.glsl"), "include is tracked");

        Files.writeString(folder.resolve("shaders/main.fsh"),
                "\uFEFF#version 150\n#include \"lib/common.glsl\"\nvoid main() {}\n");
        ShaderIncludeResolver.Result bom =
                ShaderIncludeResolver.resolve(folder, false, "shaders/main.fsh");
        check(bom.successful() && !bom.source().startsWith("\uFEFF")
                        && bom.source().startsWith("#version 150"),
                "UTF-8 BOM is removed before GLSL processing");

        Files.writeString(folder.resolve("shaders/main.fsh"),
                "#version 150\r\n#include \"lib/common.glsl\"\r\nvoid main() {}\r\n");
        ShaderIncludeResolver.Result crlf =
                ShaderIncludeResolver.resolve(folder, false, "shaders/main.fsh");
        check(crlf.successful() && crlf.source().contains("vec3 helper()"),
                "CRLF include directive resolves");

        Files.writeString(folder.resolve("shaders/main.fsh"),
                "#include \"shaders/lib/common.glsl\"\nvoid main() {}\n");
        ShaderIncludeResolver.Result rooted =
                ShaderIncludeResolver.resolve(folder, false, "shaders/main.fsh");
        check(rooted.successful() && rooted.source().contains("vec3 helper()"),
                "pack-root shaders include resolves");

        Files.writeString(shaders.resolve("common.glsl"), "#include \"nested.glsl\"\n");
        Files.writeString(shaders.resolve("nested.glsl"), "float nestedValue = 1.0;\n");
        ShaderIncludeResolver.Result nested =
                ShaderIncludeResolver.resolve(folder, false, "shaders/main.fsh");
        check(nested.successful() && nested.source().contains("nestedValue"), "nested include resolves");

        Files.writeString(shaders.resolve("common.glsl"), "#include \"nested.glsl\"\n");
        Files.writeString(shaders.resolve("nested.glsl"), "#include \"common.glsl\"\n");
        ShaderIncludeResolver.Result cycle =
                ShaderIncludeResolver.resolve(folder, false, "shaders/main.fsh");
        check(!cycle.successful() && cycle.errors().stream().anyMatch(e -> e.contains("cycle")),
                "include cycles are rejected");

        Files.writeString(folder.resolve("shaders/main.fsh"),
                "#include \"../../outside.glsl\"\nvoid main() {}\n");
        ShaderIncludeResolver.Result traversal =
                ShaderIncludeResolver.resolve(folder, false, "shaders/main.fsh");
        check(!traversal.successful(), "path traversal is rejected");

        Path zipPath = Files.createTempFile("opticore-include", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            add(zip, "shaders/main.fsh", "#include \"lib/common.glsl\"\nvoid main() {}\n");
            add(zip, "shaders/lib/common.glsl", "vec3 zipHelper() { return vec3(0.0); }\n");
        }
        ShaderIncludeResolver.Result zipResult =
                ShaderIncludeResolver.resolve(zipPath, true, "shaders/main.fsh");
        check(zipResult.successful() && zipResult.source().contains("zipHelper"), "ZIP include resolves");

        try (var paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
        Files.deleteIfExists(zipPath);
        System.out.println("ShaderIncludeResolverSelfTest: 10 checks passed");
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
