#version 150

uniform sampler2D uSceneColor;
uniform sampler2D uSceneDepth;
uniform float uWarmth;
uniform float uFogStrength;
uniform mat4 uInverseProjection;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec4 scene = texture(uSceneColor, vUv);
    vec3 warmed = scene.rgb * vec3(1.0 + uWarmth, 1.0, 1.0 - uWarmth);

    float depth = texture(uSceneDepth, vUv).r;
    vec4 clipPosition = vec4(vUv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPosition = uInverseProjection * clipPosition;
    float viewDistance = abs(viewPosition.z / max(abs(viewPosition.w), 1.0e-6));
    float haze = smoothstep(48.0, 192.0, viewDistance) * uFogStrength;

    vec3 fogColor = vec3(0.62, 0.70, 0.78);
    fragColor = vec4(mix(warmed, fogColor, haze), scene.a);
}
