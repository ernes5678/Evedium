package io.github.user.universalnvidium.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ExtensionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ExtensionManager");

    private static Set<String> availableExtensions = new HashSet<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        String extensions = GlStateManager.getExtensions();
        if (extensions != null) {
            for (String ext : extensions.split(" ")) {
                availableExtensions.add(ext.trim());
            }
        }

        LOGGER.info("Available OpenGL extensions: {}", availableExtensions.size());
        initialized = true;
    }

    public static boolean hasExtension(String extension) {
        if (!initialized) initialize();
        return availableExtensions.contains(extension);
    }

    public static boolean hasAllExtensions(String... extensions) {
        for (String ext : extensions) {
            if (!hasExtension(ext)) return false;
        }
        return true;
    }

    public static boolean hasAnyExtension(String... extensions) {
        for (String ext : extensions) {
            if (hasExtension(ext)) return true;
        }
        return false;
    }

    public static boolean supportsMeshShaders() {
        return hasExtension("GL_NV_mesh_shader");
    }

    public static boolean supportsBindlessTextures() {
        return hasExtension("GL_NV_bindless_texture") ||
               hasExtension("GL_ARB_bindless_texture");
    }

    public static boolean supportsMultiDrawIndirect() {
        return hasExtension("GL_ARB_multi_draw_indirect") &&
               hasExtension("GL_ARB_draw_indirect");
    }

    public static boolean supportsPersistentBuffers() {
        return hasExtension("GL_ARB_buffer_storage") &&
               hasExtension("GL_ARB_shader_storage_buffer_object");
    }

    public static boolean supportsComputeShaders() {
        return hasExtension("GL_ARB_compute_shader");
    }

    public static boolean supportsVertexBufferUnified() {
        return hasExtension("GL_NV_vertex_buffer_unified_memory");
    }

    public static boolean supportsConditionalRender() {
        return hasExtension("GL_NV_conditional_render") ||
               hasExtension("GL_ARB_conditional_render_inverted");
    }

    public static boolean supportsDrawBaseVertex() {
        return hasExtension("GL_ARB_draw_elements_base_vertex");
    }
}
