package io.github.user.universalnvidium;

import io.github.user.universalnvidium.gpu.GPUScanner;
import io.github.user.universalnvidium.gpu.GPUArchitecture;
import io.github.user.universalnvidium.render.backend.PascalRenderBackend;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniversalNvidiumMod implements ClientModInitializer {
    public static final String MOD_ID = "universal_nvidium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static GPUArchitecture currentArchitecture;
    private static boolean active = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Universal Nvidium initializing...");

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            detectGPU(client);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            shutdown();
        });
    }

    private void detectGPU(MinecraftClient client) {
        String renderer = client.getWindow().getGlDebugRenderer();
        String vendor = client.getWindow().getGlDebugVendor();
        String version = client.getWindow().getGlDebugVersion();

        LOGGER.info("OpenGL Renderer: {}", renderer);
        LOGGER.info("OpenGL Vendor: {}", vendor);
        LOGGER.info("OpenGL Version: {}", version);

        currentArchitecture = GPUScanner.detectArchitecture(renderer, vendor, version);
        LOGGER.info("Detected GPU Architecture: {}", currentArchitecture);

        if (currentArchitecture == GPUArchitecture.UNKNOWN ||
            currentArchitecture == GPUArchitecture.UNSUPPORTED) {
            LOGGER.warn("Universal Nvidium: Unsupported GPU detected. Disabling.");
            active = false;
            return;
        }

        if (currentArchitecture.supportsMeshShaders()) {
            active = true;
            LOGGER.info("Turing+ GPU detected. Mesh shader pipeline available. Nvidium will handle rendering.");
        } else if (currentArchitecture == GPUArchitecture.PASCAL) {
            active = true;
            LOGGER.info("Pascal GPU detected (GTX 1050 Ti compatible). Using Pascal accelerated pipeline.");
        } else {
            active = true;
            LOGGER.info("Legacy GPU detected. Using fallback pipeline.");
        }
    }

    private void shutdown() {
        if (active && PascalRenderBackend.isInitialized()) {
            PascalRenderBackend.getInstance().close();
        }
        active = false;
        LOGGER.info("Universal Nvidium shut down");
    }

    public static boolean isActive() {
        return active;
    }

    public static GPUArchitecture getArchitecture() {
        return currentArchitecture;
    }

    public static boolean isNvidiaGPU() {
        return currentArchitecture != null &&
               currentArchitecture != GPUArchitecture.UNKNOWN &&
               currentArchitecture != GPUArchitecture.UNSUPPORTED;
    }
}
