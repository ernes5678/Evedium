package io.github.user.evedium.mixin;

import io.github.user.evedium.EvediumMod;
import io.github.user.evedium.render.backend.PascalRenderBackend;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public class MixinSodiumWorldRenderer {
    @Unique
    private boolean evedium$initialized = false;

    @Inject(method = "setupTerrain", at = @At("HEAD"), remap = false)
    private void onSetupTerrain(Camera camera, Viewport viewport, FogParameters fogParameters,
                                boolean spectator, boolean booleanParam, ChunkRenderMatrices matrices,
                                CallbackInfo ci) {
        if (!EvediumMod.isActive()) return;
        if (!EvediumMod.getArchitecture().supportsMeshShaders()) {
            initBackend();
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), remap = false)
    private void onDestroy(CallbackInfo ci) {
        if (!UniversalNvidiumMod.isActive()) return;
        PascalRenderBackend.getInstance().close();
        evedium$initialized = false;
    }

    @Unique
    private void initBackend() {
        if (evedium$initialized) return;
        evedium$initialized = true;
        PascalRenderBackend.getInstance().init();
    }
}
