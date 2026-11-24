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

        ShaderProgramDescriptor.Descriptor descriptor =
                ShaderProgramDescriptor.inspect(pack.path(), pack.archive(), programId);
        if (!descriptor.readyForCompiler()) {
            List<String> diagnostics = new ArrayList<>(descriptor.diagnostics());
            if (diagnostics.isEmpty()) diagnostics.add("Program source did not pass bounded preflight");
            return new Result(false, pack.name(), programId, null, diagnostics);
        }

        String cacheKey = pack.path().toAbsolutePath().normalize() + "::" + programId;
        try {
            GlShaderProgram program = GlShaderProgramCache.getOrCompile(
                    cacheKey, descriptor.vertex().expandedSource(), descriptor.fragment().expandedSource());
            return new Result(true, pack.name(), programId, program,
                    List.of("GLSL compile and link succeeded on the current graphics context"));
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
