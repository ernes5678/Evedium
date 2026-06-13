#version 430 core
#extension GL_ARB_shader_draw_parameters : require

#include "include/constants.glsl"
#include "include/uniforms.glsl"

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec4 aColor;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec2 aLightCoord;
layout(location = 4) in vec3 aNormal;

out vec4 vColor;
out vec2 vTexCoord;
out vec2 vLightCoord;
out vec3 vNormal;
out vec3 vFragPos;
flat out uint vSectionId;

void main() {
    vSectionId = gl_BaseInstanceARB;

    vec4 worldPos = vec4(aPos, 1.0);
    vFragPos = worldPos.xyz;

    gl_Position = ubo.uProjectionMatrix * ubo.uViewMatrix * worldPos;

    vColor = aColor;
    vTexCoord = aTexCoord;
    vLightCoord = aLightCoord;
    vNormal = aNormal;
}
