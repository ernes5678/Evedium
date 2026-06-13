package io.github.user.evedium.render.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IndirectCommandBuffer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("IndirectCommandBuffer");

    public static final int DRAW_ELEMENTS_COMMAND_SIZE = 20;
    public static final int DRAW_ELEMENTS_INDIRECT_COMMAND_SIZE = 20;

    private final int glBufferId;
    private final int maxCommands;
    private long mappedAddress;
    private int commandCount;
    private final boolean useBaseVertex;

    public IndirectCommandBuffer(int maxCommands, boolean useBaseVertex) {
        this.maxCommands = maxCommands;
        this.useBaseVertex = useBaseVertex;
        long bufferSize = (long) maxCommands * DRAW_ELEMENTS_COMMAND_SIZE;

        this.glBufferId = GlStateManager.glGenBuffers();
        GlStateManager._glBindBuffer(GL44.GL_DRAW_INDIRECT_BUFFER, this.glBufferId);

        int flags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT |
                    GL44.GL_MAP_COHERENT_BIT | GL44.GL_DYNAMIC_STORAGE_BIT;
        GL44.glBufferStorage(GL44.GL_DRAW_INDIRECT_BUFFER, bufferSize, flags);

        this.mappedAddress = GL44.glMapBufferRange(
            GL44.GL_DRAW_INDIRECT_BUFFER, 0, bufferSize,
            GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT
        );

        if (this.mappedAddress == MemoryUtil.NULL) {
            GlStateManager._glBindBuffer(GL44.GL_DRAW_INDIRECT_BUFFER, 0);
            throw new RuntimeException("Failed to map indirect command buffer");
        }

        GlStateManager._glBindBuffer(GL44.GL_DRAW_INDIRECT_BUFFER, 0);
        this.commandCount = 0;
    }

    public void begin() {
        this.commandCount = 0;
    }

    public void addDrawCommand(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
        if (commandCount >= maxCommands) return;

        long offset = (long) commandCount * DRAW_ELEMENTS_COMMAND_SIZE;
        long ptr = this.mappedAddress + offset;

        MemoryUtil.memPutInt(ptr, count);
        MemoryUtil.memPutInt(ptr + 4, instanceCount);
        MemoryUtil.memPutInt(ptr + 8, firstIndex);
        if (useBaseVertex) {
            MemoryUtil.memPutInt(ptr + 12, baseVertex);
        } else {
            MemoryUtil.memPutInt(ptr + 12, 0);
        }
        MemoryUtil.memPutInt(ptr + 16, baseInstance);

        commandCount++;
    }

    public void bind() {
        GlStateManager._glBindBuffer(GL44.GL_DRAW_INDIRECT_BUFFER, this.glBufferId);
    }

    public void unbind() {
        GlStateManager._glBindBuffer(GL44.GL_DRAW_INDIRECT_BUFFER, 0);
    }

    public void multiDrawElements(int mode, int primcount) {
        if (commandCount == 0) return;

        bind();

        if (useBaseVertex) {
            GL44.glMultiDrawElementsIndirect(mode, GL44.GL_UNSIGNED_INT, 0, commandCount, 0);
        } else {
            GL44.glMultiDrawElementsIndirect(mode, GL44.GL_UNSIGNED_INT, 0, commandCount, 0);
        }

        unbind();
    }

    public void multiDrawElementsBaseVertex(int mode, ByteBuffer indirect, int primcount, int stride) {
        if (commandCount == 0) return;
        bind();
        GL44.glMultiDrawElementsIndirect(mode, GL44.GL_UNSIGNED_INT, 0, commandCount, 0);
        unbind();
    }

    public int getCommandCount() {
        return commandCount;
    }

    public void clear() {
        this.commandCount = 0;
        if (this.mappedAddress != MemoryUtil.NULL) {
            MemoryUtil.memSet(this.mappedAddress, 0, (long) maxCommands * DRAW_ELEMENTS_COMMAND_SIZE);
        }
    }

    @Override
    public void close() {
        if (this.glBufferId != 0) {
            if (this.mappedAddress != MemoryUtil.NULL) {
                GlStateManager._glBindBuffer(GL44.GL_DRAW_INDIRECT_BUFFER, this.glBufferId);
                GL44.glUnmapBuffer(GL44.GL_DRAW_INDIRECT_BUFFER);
                this.mappedAddress = MemoryUtil.NULL;
            }
            GlStateManager._glDeleteBuffers(this.glBufferId);
        }
    }
}
