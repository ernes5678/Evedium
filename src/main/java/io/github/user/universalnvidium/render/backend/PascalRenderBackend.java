package io.github.user.universalnvidium.render.backend;

import io.github.user.universalnvidium.mixin.BuilderTaskOutputAccessor;
import io.github.user.universalnvidium.render.buffer.BindlessManager;
import io.github.user.universalnvidium.render.buffer.IndirectCommandBuffer;
import io.github.user.universalnvidium.render.shader.ShaderManager;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PascalRenderBackend implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("PascalRenderBackend");
    private static PascalRenderBackend instance;

    private boolean initialized = false;

    private BindlessManager bindlessManager;
    private ShaderManager shaderManager;
    private IndirectCommandBuffer cmdBuffer;

    private final Set<RenderSection> trackedSections = ConcurrentHashMap.newKeySet();

    static final int MAX_SECTIONS = 4096;
    static final int MAX_COMMANDS = 4096;

    private long frameIndex = 0;

    public static PascalRenderBackend getInstance() {
        if (instance == null) instance = new PascalRenderBackend();
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null && instance.initialized;
    }

    public synchronized void init() {
        if (initialized) return;
        LOGGER.info("Initializing Pascal acceleration backend...");

        MinecraftClient.getInstance();
        cmdBuffer = new IndirectCommandBuffer(MAX_COMMANDS, true);
        bindlessManager = new BindlessManager();
        bindlessManager.initialize();
        shaderManager = new ShaderManager();

        initialized = true;
        LOGGER.info("Pascal acceleration backend initialized");
    }

    public void onChunkUpload(BuilderTaskOutput output) {
        if (!initialized) return;

        RenderSection section = ((BuilderTaskOutputAccessor) output).getRenderSection();
        if (section == null || section.isDisposed()) return;

        trackedSections.add(section);
    }

    public void onSectionDelete(RenderSection section) {
        trackedSections.remove(section);
    }

    public void onRenderLayer(TerrainRenderPass pass, SortedRenderLists renderLists, int frame) {
    }

    public void onRenderFrame(int frame, boolean isTranslucent) {
        frameIndex++;
    }

    public int getTrackedSectionCount() {
        return trackedSections.size();
    }

    public long getFrameIndex() {
        return frameIndex;
    }

    @Override
    public synchronized void close() {
        if (!initialized) return;
        LOGGER.info("Shutting down Pascal acceleration backend");

        try { cmdBuffer.close(); } catch (Exception ignored) {}
        try { bindlessManager.close(); } catch (Exception ignored) {}
        try { shaderManager.close(); } catch (Exception ignored) {}

        trackedSections.clear();
        initialized = false;
        LOGGER.info("Pascal acceleration backend shut down");
    }
}
