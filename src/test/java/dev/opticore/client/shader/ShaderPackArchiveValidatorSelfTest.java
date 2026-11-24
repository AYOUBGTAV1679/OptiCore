package dev.opticore.client.shader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShaderPackArchiveValidatorSelfTest {
    private ShaderPackArchiveValidatorSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path valid = Files.createTempFile("opticore-shaders-valid", ".zip");
        writeZip(valid, "shaders/gbuffers_terrain.vsh");
        try (var zip = new java.util.zip.ZipFile(valid.toFile())) {
            check(ShaderPackArchiveValidator.containsRootShaderDirectory(zip), "root shaders directory detected");
        }

        Path nested = Files.createTempFile("opticore-shaders-nested", ".zip");
        writeZip(nested, "SomePack/shaders/gbuffers_terrain.vsh");
        try (var zip = new java.util.zip.ZipFile(nested.toFile())) {
            check(!ShaderPackArchiveValidator.containsRootShaderDirectory(zip), "nested shaders directory rejected");
        }

        check(!ShaderPackArchiveValidator.isSafeEntryName("../shaders/evil.fsh"), "parent traversal rejected");
        check(!ShaderPackArchiveValidator.isSafeEntryName("shaders\\..\\evil.fsh"), "backslash path rejected");
        check(!ShaderPackArchiveValidator.isSafeEntryName("/shaders/evil.fsh"), "absolute path rejected");
        check(ShaderPackArchiveValidator.isSafeEntryName("shaders/composite.fsh"), "normal shader path accepted");


        Path crowded = Files.createTempFile("opticore-shaders-crowded", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(crowded))) {
            output.putNextEntry(new ZipEntry("shaders/first.fsh"));
            output.write(1);
            output.closeEntry();
            for (int i = 1; i < ShaderPackArchiveValidator.MAX_ENTRIES; i++) {
                output.putNextEntry(new ZipEntry("assets/file" + i));
                output.write(1);
                output.closeEntry();
            }
        }
        try (var zip = new java.util.zip.ZipFile(crowded.toFile())) {
            check(ShaderPackArchiveValidator.containsRootShaderDirectory(zip), "entry count at limit accepted");
        }
        Path overLimit = Files.createTempFile("opticore-shaders-over-limit", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(overLimit))) {
            output.putNextEntry(new ZipEntry("shaders/first.fsh"));
            output.write(1);
            output.closeEntry();
            for (int i = 1; i <= ShaderPackArchiveValidator.MAX_ENTRIES; i++) {
                output.putNextEntry(new ZipEntry("assets/file" + i));
                output.write(1);
                output.closeEntry();
            }
        }
        try (var zip = new java.util.zip.ZipFile(overLimit.toFile())) {
            check(!ShaderPackArchiveValidator.containsRootShaderDirectory(zip), "entry count over limit rejected");
        }

        Files.deleteIfExists(valid);
        Files.deleteIfExists(nested);
        Files.deleteIfExists(crowded);
        Files.deleteIfExists(overLimit);
        System.out.println("ShaderPackArchiveValidatorSelfTest: 8 checks passed");
    }

    private static void writeZip(Path file, String entryName) throws Exception {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(file))) {
            output.putNextEntry(new ZipEntry(entryName));
            output.write(new byte[] { 1, 2, 3 });
            output.closeEntry();
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
