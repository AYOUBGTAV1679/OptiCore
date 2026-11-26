package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Conservative contract for the one fullscreen scene-composite path currently
 * implemented by OptiCore. Passing this check does not prove GLSL compilation
 * or general shader-pack compatibility.
 */
public final class FullscreenShaderContract {
    private static final Pattern VERTEX_ID = Pattern.compile("\\bgl_VertexID\\b");
    private static final Map<String, String> REQUIRED_FRAGMENT_UNIFORMS = Map.ofEntries(
            Map.entry("uSceneColor", "sampler2D"),
            Map.entry("uSceneDepth", "sampler2D"),
            Map.entry("uWarmth", "float"),
            Map.entry("uFogStrength", "float"),
            Map.entry("uFogColor", "vec3"),
            Map.entry("uFogStart", "float"),
            Map.entry("uFogEnd", "float"),
            Map.entry("uExposure", "float"),
            Map.entry("uContrast", "float"),
            Map.entry("uSaturation", "float"),
            Map.entry("uInverseProjection", "mat4"));

    private FullscreenShaderContract() {}

    public record Result(boolean supported, List<String> diagnostics) {
        public Result { diagnostics = List.copyOf(diagnostics); }
    }

    public static Result validate(String vertexSource, String fragmentSource) {
        List<String> errors = new ArrayList<>();
        if (vertexSource == null || vertexSource.isBlank()) {
            return new Result(false, List.of("vertex source is missing"));
        }
        if (fragmentSource == null || fragmentSource.isBlank()) {
            return new Result(false, List.of("fragment source is missing"));
        }

        ShaderGlslInterfaceScanner.Summary vertex = ShaderGlslInterfaceScanner.inspect(vertexSource);
        ShaderGlslInterfaceScanner.Summary fragment = ShaderGlslInterfaceScanner.inspect(fragmentSource);
        if (vertex.truncated() || fragment.truncated()) {
            errors.add("shader interface inventory is truncated");
        }
        if (!vertex.duplicateUniformNames().isEmpty() || !fragment.duplicateUniformNames().isEmpty()) {
            errors.add("duplicate uniform declarations are not supported by this contract");
        }
        if (!VERTEX_ID.matcher(vertexSource).find()) {
            errors.add("vertex stage must generate the fullscreen triangle from gl_VertexID");
        }
        if (!vertex.inputs().isEmpty()) {
            errors.add("fullscreen vertex stage must not require vertex attributes");
        }
        requireSingleInterface(vertex.outputs(), "vUv", "vec2", "vertex output", errors);
        requireSingleInterface(fragment.inputs(), "vUv", "vec2", "fragment input", errors);
        requireSingleInterface(fragment.outputs(), "fragColor", "vec4", "fragment output", errors);

        if (!vertex.uniforms().isEmpty()) {
            errors.add("vertex-stage uniforms are outside the supported fullscreen contract");
        }
        Map<String, String> declared = new LinkedHashMap<>();
        for (ShaderGlslInterfaceScanner.Declaration uniform : fragment.uniforms()) {
            if (!uniform.arraySuffix().isEmpty()) {
                errors.add("uniform arrays are not supported: " + uniform.name());
            }
            declared.put(uniform.name(), uniform.type());
        }
        for (Map.Entry<String, String> required : REQUIRED_FRAGMENT_UNIFORMS.entrySet()) {
            String actual = declared.remove(required.getKey());
            if (actual == null) {
                errors.add("missing required fragment uniform " + required.getKey());
            } else if (!required.getValue().equals(actual)) {
                errors.add("uniform " + required.getKey() + " must be " + required.getValue()
                        + " but is " + actual);
            }
        }
        for (String unsupported : declared.keySet()) {
            errors.add("unsupported fragment uniform " + unsupported);
        }
        return new Result(errors.isEmpty(), errors);
    }

    private static void requireSingleInterface(List<ShaderGlslInterfaceScanner.Declaration> declarations,
            String expectedName, String expectedType, String label, List<String> errors) {
        if (declarations.size() != 1) {
            errors.add(label + " must declare exactly " + expectedType + " " + expectedName);
            return;
        }
        ShaderGlslInterfaceScanner.Declaration declaration = declarations.get(0);
        if (!expectedName.equals(declaration.name()) || !expectedType.equals(declaration.type())
                || !declaration.arraySuffix().isEmpty()) {
            errors.add(label + " must declare exactly " + expectedType + " " + expectedName);
        }
    }
}
