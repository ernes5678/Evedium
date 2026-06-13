package io.github.user.universalnvidium.render.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BindlessManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("BindlessManager");

    private final Map<Integer, long[]> textureHandles = new HashMap<>();
    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("Bindless texture manager initialized");
    }

    public long getTextureHandle(int glTextureId) {
        if (!initialized) initialize();

        return textureHandles.computeIfAbsent(glTextureId, id -> {
            long handle = GL44.glGetTextureHandleNV(id);
            if (handle == 0) {
                handle = GL44.glGetTextureHandleARB(id);
            }
            if (handle != 0) {
                GL44.glMakeTextureHandleResidentNV(handle);
            }
            return new long[]{handle};
        })[0];
    }

    public long getTextureHandle(int glTextureId, int samplerId) {
        if (!initialized) initialize();

        long key = ((long) glTextureId << 32) | (samplerId & 0xFFFFFFFFL);
        return textureHandles.computeIfAbsent((int)(key >>> 32) ^ samplerId, k -> {
            long handle = GL44.glGetTextureSamplerHandleNV(glTextureId, samplerId);
            if (handle == 0) {
                handle = GL44.glGetTextureSamplerHandleARB(glTextureId, samplerId);
            }
            if (handle != 0) {
                GL44.glMakeTextureHandleResidentNV(handle);
            }
            return new long[]{handle};
        })[0];
    }

    public void makeResident(long handle) {
        GL44.glMakeTextureHandleResidentNV(handle);
    }

    public void makeNonResident(long handle) {
        GL44.glMakeTextureHandleNonResidentNV(handle);
    }

    public void bindHandleToUnit(int textureUnit, long handle) {
        GL44.glBindTextureUnit(textureUnit, (int)(handle & 0xFFFFFFFFL));
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void close() {
        if (!initialized) return;
        for (var entry : textureHandles.entrySet()) {
            long[] handles = entry.getValue();
            for (long handle : handles) {
                if (handle != 0) {
                    try {
                        GL44.glMakeTextureHandleNonResidentNV(handle);
                    } catch (Exception ignored) {}
                }
            }
        }
        textureHandles.clear();
        initialized = false;
    }
}
