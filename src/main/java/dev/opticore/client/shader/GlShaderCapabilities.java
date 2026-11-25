package dev.opticore.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Immutable snapshot of the current OpenGL context capabilities.
 *
 * <p>Must be queried on Minecraft's render thread with a current context.
 * This is a conservative gate for OptiCore's GLSL 1.50-era pipeline, not a
 * guarantee that a particular shader pack is compatible.</p>
 */
public record GlShaderCapabilities(
        String version,
        String shadingLanguageVersion,
        String vendor,
        String renderer,
        int majorVersion,
        int minorVersion,
        int maxCombinedTextureUnits) {

    public static GlShaderCapabilities query() {
        RenderSystem.assertOnRenderThread();
        String version = safe(GL11.glGetString(GL11.GL_VERSION));
        String shading = safe(GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        String vendor = safe(GL11.glGetString(GL11.GL_VENDOR));
        String renderer = safe(GL11.glGetString(GL11.GL_RENDERER));
        int[] parsed = parseVersion(version);
        int textureUnits = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        return new GlShaderCapabilities(version, shading, vendor, renderer,
                parsed[0], parsed[1], textureUnits);
    }

    /** GLSL 1.50 shaders require at least an OpenGL 3.2 context. */
    public boolean supportsCoreShaderPipeline() {
        return majorVersion > 3 || (majorVersion == 3 && minorVersion >= 2);
    }

    public String compatibilityDiagnostic() {
        if (supportsCoreShaderPipeline()) {
            return "OpenGL " + majorVersion + "." + minorVersion
                    + " supports the OptiCore GLSL 1.50 baseline";
        }
        return "OpenGL 3.2 is required for the OptiCore GLSL 1.50 baseline; detected "
                + (version.isBlank() ? "unknown version" : version);
    }

    private static int[] parseVersion(String value) {
        if (value == null) return new int[]{0, 0};
        // Drivers may prefix the version with vendor text; find the first
        // numeric major.minor pair rather than assuming a fixed prefix.
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)").matcher(value);
        if (!matcher.find()) return new int[]{0, 0};
        try {
            return new int[]{Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))};
        } catch (NumberFormatException ignored) {
            return new int[]{0, 0};
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
