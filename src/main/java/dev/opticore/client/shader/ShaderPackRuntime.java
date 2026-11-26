package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.IntBuffer;

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

    /**
     * Compiles the first selected-pack program that satisfies OptiCore's narrow
     * fullscreen world-composite contract. This is intentionally not a general
     * OptiFine shader-pack compatibility claim.
     */
    public static Result compileSelectedFullscreenProgram() {
        RenderSystem.assertOnRenderThread();
        String selected = ShaderPackRepository.activePack();
        if ("OFF".equalsIgnoreCase(selected)) {
            return failure("OFF", "", "No shader pack is selected");
        }
        ShaderPackRepository.Pack pack = ShaderPackRepository.discover().stream()
                .filter(candidate -> candidate.name().equals(selected))
                .findFirst().orElse(null);
        if (pack == null) {
            return failure(selected, "", "Selected shader pack was not found");
        }

        ShaderPackCompatibilityReport.Summary report = inspectPackCompatibility(pack);
        List<String> rejected = new ArrayList<>();
        for (ShaderPackCompatibilityReport.Program candidate : report.programs()) {
            if (!candidate.readyForCompiler()) continue;
            ShaderProgramDescriptor.Descriptor descriptor =
                    ShaderProgramDescriptor.inspect(pack.path(), pack.archive(), candidate.id());
            ShaderOptionParser.Summary options =
                    ShaderOptionDiscovery.inspect(pack.path(), pack.archive());
            java.util.Map<String, String> overrides = ShaderPackRepository.optionOverrides(pack.name());
            ShaderOptionSourceProcessor.Result vertex = ShaderOptionSourceProcessor.apply(
                    descriptor.vertex().expandedSource(), options, overrides);
            ShaderOptionSourceProcessor.Result fragment = ShaderOptionSourceProcessor.apply(
                    descriptor.fragment().expandedSource(), options, overrides);
            FullscreenShaderContract.Result contract = FullscreenShaderContract.validate(
                    vertex.source(), fragment.source());
            if (!contract.supported()) {
                rejected.add(candidate.id() + ": " + String.join("; ", contract.diagnostics()));
                continue;
            }

            Result compiled = compileProgram(pack, candidate.id());
            if (compiled.compiled()) {
                List<String> reflectionErrors = validateLinkedFullscreenUniforms(compiled.program());
                if (!reflectionErrors.isEmpty()) {
                    rejected.add(candidate.id() + ": linked interface mismatch: "
                            + String.join("; ", reflectionErrors));
                    continue;
                }
                List<String> diagnostics = new ArrayList<>(compiled.diagnostics());
                diagnostics.add("Fullscreen world-pass contract accepted");
                if (!rejected.isEmpty()) {
                    diagnostics.add("Earlier candidates rejected: " + rejected.size());
                }
                return new Result(true, compiled.packName(), compiled.programId(),
                        compiled.program(), diagnostics);
            }
            rejected.add(candidate.id() + ": " + String.join("; ", compiled.diagnostics()));
        }
        if (rejected.isEmpty()) {
            rejected.add("No paired program passed the bounded source preflight");
        }
        if (report.truncated()) rejected.add("Pack inspection was truncated at the safety limit");
        return failure(pack.name(), "", "No usable fullscreen program: " + String.join(" | ", rejected));
    }

    private static List<String> validateLinkedFullscreenUniforms(GlShaderProgram program) {
        List<String> errors = new ArrayList<>();
        int[][] required = {
                {GL20.GL_SAMPLER_2D, GL20.glGetUniformLocation(program.id(), "uSceneColor")},
                {GL20.GL_SAMPLER_2D, GL20.glGetUniformLocation(program.id(), "uSceneDepth")},
                {GL20.GL_FLOAT, GL20.glGetUniformLocation(program.id(), "uWarmth")},
                {GL20.GL_FLOAT, GL20.glGetUniformLocation(program.id(), "uFogStrength")},
                {GL20.GL_FLOAT_VEC3, GL20.glGetUniformLocation(program.id(), "uFogColor")},
                {GL20.GL_FLOAT, GL20.glGetUniformLocation(program.id(), "uFogStart")},
                {GL20.GL_FLOAT, GL20.glGetUniformLocation(program.id(), "uFogEnd")},
                {GL20.GL_FLOAT, GL20.glGetUniformLocation(program.id(), "uExposure")},
                {GL20.GL_FLOAT, GL20.glGetUniformLocation(program.id(), "uContrast")},
                {GL20.GL_FLOAT, GL20.glGetUniformLocation(program.id(), "uSaturation")},
                {GL20.GL_FLOAT_MAT4, GL20.glGetUniformLocation(program.id(), "uInverseProjection")}
        };
        String[] names = {"uSceneColor", "uSceneDepth", "uWarmth", "uFogStrength", "uFogColor", "uFogStart", "uFogEnd", "uExposure", "uContrast", "uSaturation", "uInverseProjection"};
        for (int i = 0; i < required.length; i++) {
            if (required[i][1] < 0) errors.add(names[i] + " is inactive or missing after GL link");
        }
        if (!errors.isEmpty()) return errors;

        int activeCount = GL20.glGetProgrami(program.id(), GL20.GL_ACTIVE_UNIFORMS);
        IntBuffer size = BufferUtils.createIntBuffer(1);
        IntBuffer type = BufferUtils.createIntBuffer(1);
        java.util.Map<String, Integer> activeTypes = new java.util.HashMap<>();
        for (int index = 0; index < activeCount; index++) {
            size.clear();
            type.clear();
            String name = GL20.glGetActiveUniform(program.id(), index, 256, size, type);
            if (name == null) continue;
            String normalized = name.endsWith("[0]") ? name.substring(0, name.length() - 3) : name;
            activeTypes.put(normalized, type.get(0));
        }
        for (int i = 0; i < names.length; i++) {
            Integer actual = activeTypes.get(names[i]);
            if (actual == null || actual != required[i][0]) {
                errors.add(names[i] + " has an unexpected linked OpenGL type");
            }
        }
        return errors;
    }

    private static Result failure(String packName, String programId, String diagnostic) {
        return new Result(false, packName, programId, null, List.of(diagnostic));
    }
}
