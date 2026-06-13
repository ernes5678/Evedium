#version 430 core
#extension GL_NV_bindless_texture : require

#include "include/constants.glsl"
#include "include/uniforms.glsl"

in vec4 vColor;
in vec2 vTexCoord;
in vec2 vLightCoord;
in vec3 vNormal;
in vec3 vFragPos;
flat in uint vSectionId;

layout(location = 0) out vec4 fragColor;

vec4 sampleBlockTexture(uint sectionId, vec2 coord) {
    uvec2 handle = textureHandles[sectionId];
    sampler2D tex = sampler2D(handle);
    return texture(tex, coord);
}

void main() {
    vec4 texColor = sampleBlockTexture(vSectionId, vTexCoord);
    if (texColor.a < 0.002) discard;

    vec4 mixed = vColor * texColor;

    float torch = vLightCoord.x / 255.0;
    float sky = vLightCoord.y / 255.0;
    float light = mix(sky, 1.0, torch);

    vec3 finalColor = mixed.rgb * light;

    float fogFactor = 0.0;
    if (ubo.uFogDistance.y > 0.0) {
        float dist = length(vFragPos);
        fogFactor = clamp((dist - ubo.uFogDistance.x) / (ubo.uFogDistance.y - ubo.uFogDistance.x), 0.0, 1.0);
    }
    finalColor = mix(finalColor, ubo.uFogColor.rgb, fogFactor);

    fragColor = vec4(finalColor, mixed.a);
}
