package dev.opticore.client.shader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounded compatibility preflight for shader-pack programs.
 * READY means source assembly and conservative interface checks passed;
 * it does not mean the GLSL was compiled or installed in the world renderer.
 */
public final class ShaderPackCompatibilityReport {
    public static final int MAX_PROGRAMS_TO_INSPECT = 128;

    private ShaderPackCompatibilityReport() {}

    public record Program(String id, boolean readyForCompiler, List<String> diagnostics) {
        public Program {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public record Summary(int pairedPrograms, int inspectedPrograms, int readyPrograms,
                          int blockedPrograms, int vertexOnlyPrograms, int fragmentOnlyPrograms,
                          boolean truncated, List<Program> programs) {
        public Summary {
            programs = List.copyOf(programs);
        }

        public boolean allInspectedProgramsReady() {
            return pairedPrograms > 0 && !truncated && blockedPrograms == 0 && vertexOnlyPrograms == 0
                    && fragmentOnlyPrograms == 0 && inspectedPrograms == pairedPrograms;
        }
    }

    public static Summary inspect(Path packPath, boolean archive) {
        if (packPath == null) {
            return new Summary(0, 0, 0, 0, 0, 0, false, List.of());
        }
        ShaderProgramIndex.Summary index = ShaderProgramIndex.inspect(packPath, archive);
        List<Program> results = new ArrayList<>();
        int ready = 0;
        int blocked = 0;
        int inspected = Math.min(index.pairedPrograms(), MAX_PROGRAMS_TO_INSPECT);
        int position = 0;
        for (String id : index.paired().stream().sorted().toList()) {
            if (position++ >= MAX_PROGRAMS_TO_INSPECT) break;
            ShaderProgramDescriptor.Descriptor descriptor =
                    ShaderProgramDescriptor.inspect(packPath, archive, id);
            boolean programReady = descriptor.readyForCompiler();
            if (programReady) ready++; else blocked++;
            results.add(new Program(id, programReady, descriptor.diagnostics()));
        }
        boolean truncated = index.truncated() || index.pairedPrograms() > MAX_PROGRAMS_TO_INSPECT;
        return new Summary(index.pairedPrograms(), inspected, ready, blocked,
                index.vertexOnly(), index.fragmentOnly(), truncated, results);
    }
}
