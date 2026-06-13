package io.github.user.evedium.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.user.evedium.EvediumMod;
import io.github.user.evedium.gpu.GPUArchitecture;
import io.github.user.evedium.render.backend.PascalRenderBackend;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class EvediumConfig implements ModMenuApi {
    private static EvediumConfig instance;

    private boolean enabled = true;
    private boolean debugLogging = false;

    public static EvediumConfig getInstance() {
        if (instance == null) instance = new EvediumConfig();
        return instance;
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new EvediumScreen(parent);
    }

    public boolean isEnabled() {
        return enabled && EvediumMod.isActive();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    private static class EvediumScreen extends Screen {
        private final Screen parent;

        protected EvediumScreen(Screen parent) {
            super(Text.literal("Evedium Config"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int y = 30;

            addDrawableChild(ButtonWidget.builder(
                Text.literal("Active: " + EvediumMod.isActive()),
                btn -> {}
            ).dimensions(width / 2 - 100, y, 200, 20).build());
            y += 25;

            GPUArchitecture arch = EvediumMod.getArchitecture();
            String archName = arch != null ? arch.getDisplayName() : "Unknown";
            addDrawableChild(ButtonWidget.builder(
                Text.literal("GPU: " + archName),
                btn -> {}
            ).dimensions(width / 2 - 100, y, 200, 20).build());
            y += 25;

            String pipeline;
            if (arch != null && arch.supportsMeshShaders()) {
                pipeline = "Mesh shaders (handled by Nvidium)";
            } else if (arch == GPUArchitecture.PASCAL) {
                pipeline = "Pascal accelerated";
            } else {
                pipeline = "Sodium standard";
            }
            addDrawableChild(ButtonWidget.builder(
                Text.literal("Pipeline: " + pipeline),
                btn -> {}
            ).dimensions(width / 2 - 100, y, 200, 20).build());
            y += 25;

            addDrawableChild(ButtonWidget.builder(
                Text.literal("Mesh Shaders: " + (arch != null && arch.supportsMeshShaders())),
                btn -> {}
            ).dimensions(width / 2 - 100, y, 200, 20).build());
            y += 25;

            if (arch == GPUArchitecture.PASCAL && PascalRenderBackend.isInitialized()) {
                addDrawableChild(ButtonWidget.builder(
                    Text.literal("Tracked Sections: " + PascalRenderBackend.getInstance().getTrackedSectionCount()),
                    btn -> {}
                ).dimensions(width / 2 - 100, y, 200, 20).build());
                y += 25;
            }

            y += 15;

            addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                btn -> close()
            ).dimensions(width / 2 - 100, y, 200, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            client.setScreen(parent);
        }
    }
}
