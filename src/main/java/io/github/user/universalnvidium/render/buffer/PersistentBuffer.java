package io.github.user.universalnvidium.render.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class PersistentBuffer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("PersistentBuffer");

    private final int target;
    private final int glBufferId;
    private final long size;
    private final int flags;
    private long mappedAddress;

    public static final int FLAG_MAP_WRITE = GL44.GL_MAP_WRITE_BIT |
                                              GL44.GL_MAP_PERSISTENT_BIT |
                                              GL44.GL_MAP_COHERENT_BIT;
    public static final int FLAG_MAP_READ = GL44.GL_MAP_READ_BIT |
                                            GL44.GL_MAP_PERSISTENT_BIT |
                                            GL44.GL_MAP_COHERENT_BIT;
    public static final int FLAG_STORAGE = GL44.GL_DYNAMIC_STORAGE_BIT |
                                           GL44.GL_MAP_WRITE_BIT |
                                           GL44.GL_MAP_PERSISTENT_BIT |
                                           GL44.GL_MAP_COHERENT_BIT;

    public PersistentBuffer(int target, long size, int usageFlags) {
        this.target = target;
        this.size = size;
        this.flags = usageFlags | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;

        this.glBufferId = GlStateManager.glGenBuffers();

        GlStateManager._glBindBuffer(target, this.glBufferId);
        GL44.glBufferStorage(target, size, this.flags);

        this.mappedAddress = GL44.glMapBufferRange(
            target, 0, size,
            this.flags & (GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_READ_BIT |
                          GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT)
        );

        if (this.mappedAddress == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to map persistent buffer of size " + size);
        }

        GlStateManager._glBindBuffer(target, 0);
    }

    public PersistentBuffer(long size, int usageFlags) {
        this(GL44.GL_ARRAY_BUFFER, size, usageFlags);
    }

    public void bind(int target) {
        GlStateManager._glBindBuffer(target, this.glBufferId);
    }

    public void bindBase(int target, int index) {
        GL44.glBindBufferBase(target, index, this.glBufferId);
    }

    public long getMappedAddress() {
        return this.mappedAddress;
    }

    public int getGlBufferId() {
        return this.glBufferId;
    }

    public long getSize() {
        return this.size;
    }

    public void write(long offset, ByteBuffer data) {
        if (this.mappedAddress != MemoryUtil.NULL) {
            long dest = this.mappedAddress + offset;
            int length = data.remaining();
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), dest, length);
        }
    }

    public void clear() {
        if (this.mappedAddress != MemoryUtil.NULL) {
            MemoryUtil.memSet(this.mappedAddress, 0, this.size);
        }
    }

    @Override
    public void close() {
        if (this.glBufferId != 0) {
            if (this.mappedAddress != MemoryUtil.NULL) {
                GL44.glUnmapBuffer(GL44.GL_ARRAY_BUFFER);
                this.mappedAddress = MemoryUtil.NULL;
            }
            GlStateManager._glDeleteBuffers(this.glBufferId);
        }
    }
}
