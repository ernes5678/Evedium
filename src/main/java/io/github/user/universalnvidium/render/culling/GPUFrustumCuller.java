package io.github.user.universalnvidium.render.culling;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.user.universalnvidium.render.buffer.PersistentBuffer;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPUFrustumCuller implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("GPUFrustumCuller");

    private static final int FRUSTUM_DATA_SIZE = 6 * 4 * 4;
    private static final int CULL_OUTPUT_SIZE = 4;

    private final PersistentBuffer frustumBuffer;
    private final PersistentBuffer bboxBuffer;
    private final PersistentBuffer outputBuffer;

    private final int computeProgram;
    private int numBboxes = 0;

    public GPUFrustumCuller(int maxRegions) {
        long bboxSize = (long) maxRegions * 6 * 4 * 4;
        long outputSize = (long) maxRegions * 4;

        this.frustumBuffer = new PersistentBuffer(GL44.GL_SHADER_STORAGE_BUFFER, FRUSTUM_DATA_SIZE, PersistentBuffer.FLAG_MAP_WRITE);
        this.bboxBuffer = new PersistentBuffer(GL44.GL_SHADER_STORAGE_BUFFER, bboxSize, PersistentBuffer.FLAG_MAP_WRITE);
        this.outputBuffer = new PersistentBuffer(GL44.GL_SHADER_STORAGE_BUFFER, outputSize, PersistentBuffer.FLAG_MAP_READ);

        this.computeProgram = createCullingProgram();
    }

    private int createCullingProgram() {
        int program = GL44.glCreateProgram();
        int computeShader = GL44.glCreateShader(GL44.GL_COMPUTE_SHADER);

        String source = getCullingComputeSource();
        GL44.glShaderSource(computeShader, source);
        GL44.glCompileShader(computeShader);

        int status = GL44.glGetShaderi(computeShader, GL44.GL_COMPILE_STATUS);
        if (status != GL44.GL_TRUE) {
            String log = GL44.glGetShaderInfoLog(computeShader);
            LOGGER.error("Compute shader compilation failed: {}", log);
            GL44.glDeleteShader(computeShader);
            return 0;
        }

        GL44.glAttachShader(program, computeShader);
        GL44.glLinkProgram(program);

        status = GL44.glGetProgrami(program, GL44.GL_LINK_STATUS);
        if (status != GL44.GL_TRUE) {
            String log = GL44.glGetProgramInfoLog(program);
            LOGGER.error("Compute program linking failed: {}", log);
            GL44.glDeleteProgram(program);
            GL44.glDeleteShader(computeShader);
            return 0;
        }

        GL44.glDeleteShader(computeShader);
        return program;
    }

    public void uploadFrustum(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        Matrix4f projView = new Matrix4f(projectionMatrix);
        projView.mul(viewMatrix);

        FrustumIntersection frustum = new FrustumIntersection(projView);

        float[] frustumData = new float[24];
        float[][] planes = {
            {frustum.plane(FrustumIntersection.PLANE_NX)},
            {frustum.plane(FrustumIntersection.PLANE_PX)},
            {frustum.plane(FrustumIntersection.PLANE_NY)},
            {frustum.plane(FrustumIntersection.PLANE_PY)},
            {frustum.plane(FrustumIntersection.PLANE_NZ)},
            {frustum.plane(FrustumIntersection.PLANE_PZ)}
        };

        int idx = 0;
        for (float[] plane : planes) {
            frustumData[idx++] = plane[0];
            frustumData[idx++] = plane[1];
            frustumData[idx++] = plane[2];
            frustumData[idx++] = plane[3];
        }

        long addr = this.frustumBuffer.getMappedAddress();
        for (int i = 0; i < frustumData.length; i++) {
            MemoryUtil.memPutFloat(addr + (long) i * 4, frustumData[i]);
        }
    }

    public void uploadBboxes(float[] bboxData, int count) {
        this.numBboxes = count;
        long addr = this.bboxBuffer.getMappedAddress();
        for (int i = 0; i < count * 6; i++) {
            MemoryUtil.memPutFloat(addr + (long) i * 4, bboxData[i]);
        }
    }

    public void cull() {
        if (computeProgram == 0 || numBboxes == 0) return;

        GL44.glUseProgram(computeProgram);

        int frustumBinding = 0;
        int bboxBinding = 1;
        int outputBinding = 2;

        this.frustumBuffer.bindBase(GL44.GL_SHADER_STORAGE_BUFFER, frustumBinding);
        this.bboxBuffer.bindBase(GL44.GL_SHADER_STORAGE_BUFFER, bboxBinding);
        this.outputBuffer.bindBase(GL44.GL_SHADER_STORAGE_BUFFER, outputBinding);

        int workGroupSize = 64;
        int groups = (numBboxes + workGroupSize - 1) / workGroupSize;

        GL44.glDispatchCompute(groups, 1, 1);
        GL44.glMemoryBarrier(GL44.GL_SHADER_STORAGE_BARRIER_BIT |
                            GL44.GL_COMMAND_BARRIER_BIT);

        GL44.glUseProgram(0);
    }

    public int[] getVisibleResults() {
        if (numBboxes == 0) return new int[0];

        int[] results = new int[numBboxes];
        long addr = this.outputBuffer.getMappedAddress();
        for (int i = 0; i < numBboxes; i++) {
            results[i] = MemoryUtil.memGetInt(addr + (long) i * 4);
        }
        return results;
    }

    private String getCullingComputeSource() {
        return """
            #version 430
            #extension GL_ARB_compute_shader : require
            #extension GL_ARB_shader_storage_buffer_object : require

            layout(local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

            struct Plane {
                vec4 equation;
            };

            struct BoundingBox {
                vec4 minData;
                vec2 maxData;
            };

            layout(std430, binding = 0) buffer FrustumBuffer {
                Plane frustumPlanes[6];
            };

            layout(std430, binding = 1) buffer BBoxBuffer {
                BoundingBox bboxes[];
            };

            layout(std430, binding = 2) buffer OutputBuffer {
                int visible[];
            };

            bool isBboxVisible(vec3 bmin, vec3 bmax) {
                for (int i = 0; i < 6; i++) {
                    Plane p = frustumPlanes[i];
                    vec3 positive = vec3(
                        p.equation.x > 0.0 ? bmax.x : bmin.x,
                        p.equation.y > 0.0 ? bmax.y : bmin.y,
                        p.equation.z > 0.0 ? bmax.z : bmin.z
                    );
                    float d = dot(p.equation.xyz, positive) + p.equation.w;
                    if (d < 0.0) return false;
                }
                return true;
            }

            void main() {
                uint idx = gl_GlobalInvocationID.x;
                if (idx >= bboxes.length()) return;

                vec3 bmin = bboxes[idx].minData.xyz;
                vec3 bmax = vec3(bboxes[idx].maxData.x, bboxes[idx].maxData.y, bboxes[idx].minData.w);

                visible[idx] = isBboxVisible(bmin, bmax) ? 1 : 0;
            }
            """;
    }

    @Override
    public void close() {
        if (computeProgram != 0) {
            GL44.glDeleteProgram(computeProgram);
        }
        frustumBuffer.close();
        bboxBuffer.close();
        outputBuffer.close();
    }
}
