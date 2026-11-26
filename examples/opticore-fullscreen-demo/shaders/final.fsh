#version 150

#define WARMTH_SCALE 1.0 // [0.5 1.0 1.5 2.0] Controls the strength of OptiCore warmth.
#define EXPOSURE_SCALE 1.0 // [0.5 0.75 1.0 1.25 1.5] Adjusts scene exposure before atmospheric fog.
#define CONTRAST_SCALE 1.0 // [0.75 0.9 1.0 1.1 1.25] Adjusts scene contrast around middle gray.
#define SATURATION_SCALE 1.0 // [0.0 0.5 1.0 1.5] Adjusts scene color saturation.
#define FOG_NEAR 48.0 // [24.0 48.0 72.0 96.0] Distance where atmospheric haze begins.
#define FOG_FAR 192.0 // [96.0 144.0 192.0 256.0] Distance where atmospheric haze reaches full strength.

uniform sampler2D uSceneColor;
uniform sampler2D uSceneDepth;
uniform float uWarmth;
uniform float uFogStrength;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;
uniform float uExposure;
uniform float uContrast;
uniform float uSaturation;
uniform mat4 uInverseProjection;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec4 scene = texture(uSceneColor, vUv);
    float warmth = uWarmth * WARMTH_SCALE;
    vec3 warmed = scene.rgb * vec3(1.0 + warmth, 1.0, 1.0 - warmth);
    vec3 exposed = warmed * EXPOSURE_SCALE;
    vec3 contrasted = (exposed - vec3(0.5)) * CONTRAST_SCALE + vec3(0.5);
    float luminance = dot(contrasted, vec3(0.2126, 0.7152, 0.0722));
    vec3 graded = mix(vec3(luminance), contrasted, SATURATION_SCALE);
    vec3 globalExposed = graded * uExposure;
    vec3 globalContrasted = (globalExposed - vec3(0.5)) * uContrast + vec3(0.5);
    float globalLuminance = dot(globalContrasted, vec3(0.2126, 0.7152, 0.0722));
    vec3 globallyGraded = mix(vec3(globalLuminance), globalContrasted, uSaturation);

    float depth = texture(uSceneDepth, vUv).r;
    vec4 clipPosition = vec4(vUv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPosition = uInverseProjection * clipPosition;
    float viewDistance = abs(viewPosition.z / max(abs(viewPosition.w), 1.0e-6));
    float fogNear = max(0.0, uFogStart + FOG_NEAR - 48.0);
    float fogFar = max(fogNear + 1.0, uFogEnd + FOG_FAR - 192.0);
    float haze = smoothstep(fogNear, fogFar, viewDistance) * uFogStrength;

    fragColor = vec4(mix(globallyGraded, uFogColor, haze), scene.a);
}
