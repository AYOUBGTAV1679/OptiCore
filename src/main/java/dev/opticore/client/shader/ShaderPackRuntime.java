package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Connects the selected shader-pack catalog to bounded source assembly and
 * the render-thread GL compiler. Compilation is explicit; this class does not
 * install the program into Minecraft's world renderer.
 */
public final class ShaderPackRuntime {
    private ShaderPackRuntime() {}

    public record Result(boolean compiled, String packName, String programId,
                         GlShaderProgram program, List<String> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    /**
     * Inspects every paired program in a pack without creating GL objects.
     * This is a bounded source/interface preflight, not a compile or renderer-integration test.
     */
    public static ShaderPackCompatibilityReport.Summary inspectPackCompatibility(
            ShaderPackRepository.Pack pack) {
        if (pack == null) {
            return ShaderPackCompatibilityReport.inspect(null, false);
        }
        return ShaderPackCompatibilityReport.inspect(pack.path(), pack.archive());
    }

    /** Compiles one program from the currently selected pack. Must run on the render thread. */
    public static Result compileSelectedProgram(String programId) {
        RenderSystem.assertOnRenderThread();
        String selected = ShaderPackRepository.activePack();
        if ("OFF".equalsIgnoreCase(selected)) {
            return failure("OFF", programId, "Shader packs are disabled");
        }

        return ShaderPackRepository.discover().stream()
                .filter(pack -> pack.name().equals(selected))
                .findFirst()
                .map(pack -> compileProgram(pack, programId))
                .orElseGet(() -> failure(selected, programId,
                        "Selected shader pack was not found; choose it again in Shader Packs settings"));
    }

    /** Compiles one program from a specific discovered pack. Must run on the render thread. */
    public static Result compileProgram(ShaderPackRepository.Pack pack, String programId) {
        RenderSystem.assertOnRenderThread();
        if (pack == null) return failure("", programId, "Shader pack is missing");

        // Fail early with a useful driver diagnostic instead of allowing shader
        // compilation to fail opaquely on contexts below the GLSL 1.50 baseline.
        GlShaderCapabilities capabilities = GlShaderCapabilities.query();
        if (!capabilities.supportsCoreShaderPipeline()) {
            return new Result(false, pack.name(), programId, null,
                    List.of(capabilities.compatibilityDiagnostic()));
        }

        ShaderProgramDescriptor.Descriptor descriptor =
                ShaderProgramDescriptor.inspect(pack.path(), pack.archive(), programId);
        if (!descriptor.readyForCompiler()) {
            List<String> diagnostics = new ArrayList<>(descriptor.diagnostics());
            if (diagnostics.isEmpty()) diagnostics.add("Program source did not pass bounded preflight");
            return new Result(false, pack.name(), programId, null, diagnostics);
        }

        ShaderOptionParser.Summary options =
                ShaderOptionDiscovery.inspect(pack.path(), pack.archive());
        java.util.Map<String, String> overrides = ShaderPackRepository.optionOverrides(pack.name());
        ShaderOptionSourceProcessor.Result vertex = ShaderOptionSourceProcessor.apply(
                descriptor.vertex().expandedSource(), options, overrides);
        ShaderOptionSourceProcessor.Result fragment = ShaderOptionSourceProcessor.apply(
                descriptor.fragment().expandedSource(), options, overrides);
        String cacheKey = ShaderProgramCacheKey.create(pack.path(), programId, overrides,
                vertex.source(), fragment.source());
        try {
            GlShaderProgram program = GlShaderProgramCache.getOrCompile(
                    cacheKey, vertex.source(), fragment.source());
            List<String> diagnostics = new ArrayList<>();
            diagnostics.add("GLSL compile and link succeeded on the current graphics context");
            diagnostics.add("Driver: " + capabilities.vendor() + " | " + capabilities.renderer());
            diagnostics.add("OpenGL: " + capabilities.version()
                    + " | GLSL: " + capabilities.shadingLanguageVersion()
                    + " | texture units: " + capabilities.maxCombinedTextureUnits());
            java.util.Set<String> applied = new java.util.LinkedHashSet<>(vertex.applied());
            applied.addAll(fragment.applied());
            if (!applied.isEmpty()) diagnostics.add("Applied option overrides: " + String.join(", ", applied));
            java.util.Set<String> ignored = new java.util.LinkedHashSet<>(vertex.ignored());
            ignored.addAll(fragment.ignored());
            // An option may legitimately be declared in only one stage. A miss in
            // the other stage is not an ignored pack override if either stage applied it.
            ignored.removeAll(applied);
            if (!ignored.isEmpty()) diagnostics.add("Ignored overrides not present or not valid for this program: "
                    + String.join(", ", ignored));
            return new Result(true, pack.name(), programId, program, diagnostics);
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            return failure(pack.name(), programId,
                    message == null || message.isBlank() ? exception.getClass().getSimpleName() : message);
        }
    }

    private static Result failure(String packName, String programId, String diagnostic) {
        return new Result(false, packName, programId, null, List.of(diagnostic));
    }
}
