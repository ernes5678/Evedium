#version 430 core

layout(std140, binding = 0) uniform UniformBufferObject {
    mat4 uProjectionMatrix;
    mat4 uViewMatrix;
    vec4 uFogColor;
    vec2 uFogDistance;
    vec2 uViewportSize;
    float uTime;
} ubo;

layout(std430, binding = 3) readonly buffer TextureHandleBuffer {
    uvec2 textureHandles[];
};

layout(std430, binding = 4) readonly buffer SectionDataBuffer {
    uint vertexOffsets[];
    uint indexOffsets[];
    uint indexCounts[];
};
