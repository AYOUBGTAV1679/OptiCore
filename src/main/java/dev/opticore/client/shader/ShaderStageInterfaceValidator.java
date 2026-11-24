package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Conservative name/type/array matching between a vertex stage's outputs and
 * a fragment stage's inputs. This is diagnostic preflight, not GLSL linking.
 */
public final class ShaderStageInterfaceValidator {
    private ShaderStageInterfaceValidator() {}

    public record Mismatch(String name, String reason) {}
    public record Summary(List<String> matched, List<Mismatch> mismatches,
                          boolean vertexTruncated, boolean fragmentTruncated) {
        public Summary {
            matched = List.copyOf(matched);
            mismatches = List.copyOf(mismatches);
        }
        public boolean compatibleByInventory() {
            return mismatches.isEmpty() && !vertexTruncated && !fragmentTruncated;
        }
    }

    public static Summary compare(ShaderGlslInterfaceScanner.Summary vertex,
                                  ShaderGlslInterfaceScanner.Summary fragment) {
        if (vertex == null) vertex = ShaderGlslInterfaceScanner.Summary.empty();
        if (fragment == null) fragment = ShaderGlslInterfaceScanner.Summary.empty();

        Map<String, ShaderGlslInterfaceScanner.Declaration> outputs = index(vertex.outputs());
        Map<String, ShaderGlslInterfaceScanner.Declaration> inputs = index(fragment.inputs());
        List<String> matched = new ArrayList<>();
        List<Mismatch> mismatches = new ArrayList<>();
        for (String name : new TreeSet<>(inputs.keySet())) {
            ShaderGlslInterfaceScanner.Declaration input = inputs.get(name);
            ShaderGlslInterfaceScanner.Declaration output = outputs.get(name);
            if (output == null) {
                mismatches.add(new Mismatch(name, "fragment input has no matching vertex output"));
            } else if (!output.type().equals(input.type())
                    || !output.arraySuffix().equals(input.arraySuffix())) {
                mismatches.add(new Mismatch(name, "vertex output and fragment input type/array differ"));
            } else {
                matched.add(name);
            }
        }
        return new Summary(matched, mismatches, vertex.truncated(), fragment.truncated());
    }

    private static Map<String, ShaderGlslInterfaceScanner.Declaration> index(
            List<ShaderGlslInterfaceScanner.Declaration> declarations) {
        Map<String, ShaderGlslInterfaceScanner.Declaration> result = new HashMap<>();
        for (ShaderGlslInterfaceScanner.Declaration declaration : declarations) {
            result.putIfAbsent(declaration.name(), declaration);
        }
        return result;
    }
}
