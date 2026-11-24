package dev.opticore.client.shader;

import java.nio.file.Path;
import java.util.List;

/**
 * Builds a diagnostic descriptor for one vertex/fragment program pair.
 * It resolves bounded includes and inventories declarations, but never compiles GLSL.
 */
public final class ShaderProgramDescriptor {
    private ShaderProgramDescriptor() {}

    public record Stage(String entryPath, String expandedSource, List<String> includedFiles,
                        List<String> errors, ShaderGlslInterfaceScanner.Summary declarations,
                        boolean truncated) {
        public Stage {
            includedFiles = List.copyOf(includedFiles);
            errors = List.copyOf(errors);
        }
        public boolean sourceReady() {
            return errors.isEmpty() && !truncated && expandedSource != null && !expandedSource.isEmpty();
        }
    }

    public record Descriptor(String programId, Stage vertex, Stage fragment,
                             ShaderStageInterfaceValidator.Summary interfaceCheck,
                             List<String> diagnostics, boolean readyForCompiler) {
        public Descriptor { diagnostics = List.copyOf(diagnostics); }
    }

    public static Descriptor inspect(Path pack, boolean archive, String programId) {
        if (pack == null || !safeProgramId(programId)) {
            Stage invalid = failedStage("", "Invalid program identifier");
            return new Descriptor("", invalid, invalid,
                    ShaderStageInterfaceValidator.compare(null, null),
                    List.of("Invalid shader program identifier"), false);
        }
        String base = "shaders/" + programId;
        Stage vertex = inspectStage(pack, archive, base + ".vsh");
        Stage fragment = inspectStage(pack, archive, base + ".fsh");
        List<String> diagnostics = new java.util.ArrayList<>();
        if (!vertex.sourceReady()) diagnostics.addAll(prefix("vertex: ", vertex.errors()));
        if (!fragment.sourceReady()) diagnostics.addAll(prefix("fragment: ", fragment.errors()));
        if (!vertex.sourceReady() && vertex.errors().isEmpty()) diagnostics.add("vertex: source is empty or truncated");
        if (!fragment.sourceReady() && fragment.errors().isEmpty()) diagnostics.add("fragment: source is empty or truncated");
        ShaderStageInterfaceValidator.Summary comparison =
                ShaderStageInterfaceValidator.compare(vertex.declarations(), fragment.declarations());
        for (ShaderStageInterfaceValidator.Mismatch mismatch : comparison.mismatches()) {
            diagnostics.add("interface " + mismatch.name() + ": " + mismatch.reason());
        }
        if (comparison.vertexTruncated() || comparison.fragmentTruncated()) {
            diagnostics.add("interface inventory was truncated");
        }
        // "readyForCompiler" means only that bounded source assembly and this
        // conservative inventory found no blockers. It is not a compile/link result.
        boolean ready = vertex.sourceReady() && fragment.sourceReady()
                && comparison.compatibleByInventory();
        return new Descriptor(programId, vertex, fragment, comparison, diagnostics, ready);
    }

    private static Stage inspectStage(Path pack, boolean archive, String entry) {
        ShaderIncludeResolver.Result resolved = ShaderIncludeResolver.resolve(pack, archive, entry);
        ShaderGlslInterfaceScanner.Summary declarations =
                ShaderGlslInterfaceScanner.inspect(resolved.source());
        return new Stage(entry, resolved.source(), resolved.includedFiles(), resolved.errors(),
                declarations, resolved.truncated() || declarations.truncated());
    }

    private static Stage failedStage(String entry, String error) {
        return new Stage(entry, "", List.of(), List.of(error),
                ShaderGlslInterfaceScanner.Summary.empty(), false);
    }

    private static List<String> prefix(String prefix, List<String> values) {
        return values.stream().map(value -> prefix + value).toList();
    }

    private static boolean safeProgramId(String id) {
        return id != null && !id.isBlank() && !id.startsWith("/")
                && !id.contains("..") && !id.contains("\\")
                && !id.contains("\\0") && !id.matches("^[A-Za-z]:.*");
    }
}
