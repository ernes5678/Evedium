package io.github.user.evedium.mixin;

import io.github.user.evedium.EvediumMod;
import io.github.user.evedium.render.backend.PascalRenderBackend;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public class MixinRenderSectionManager {

    @Inject(method = "renderLayer", at = @At("HEAD"), remap = false)
    private void onRenderLayer(TerrainRenderPass pass, SortedRenderLists renderLists, int frame, CallbackInfo ci) {
        if (!EvediumMod.isActive()) return;
        if (!EvediumMod.getArchitecture().supportsMeshShaders()) {
            PascalRenderBackend.getInstance().onRenderLayer(pass, renderLists, frame);
        }
    }

    @Inject(method = "renderFrame", at = @At("HEAD"), remap = false)
    private void onRenderFrame(int frame, boolean isTranslucent, CallbackInfo ci) {
        if (!EvediumMod.isActive()) return;
        if (!EvediumMod.getArchitecture().supportsMeshShaders()) {
            PascalRenderBackend.getInstance().onRenderFrame(frame, isTranslucent);
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), remap = false)
    private void onDestroy(CallbackInfo ci) {
        if (!EvediumMod.isActive()) return;
        PascalRenderBackend.getInstance().close();
    }
}
