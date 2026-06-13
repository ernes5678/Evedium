package io.github.user.universalnvidium.mixin;

import io.github.user.universalnvidium.UniversalNvidiumMod;
import io.github.user.universalnvidium.render.backend.PascalRenderBackend;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager", remap = false)
public class MixinRenderRegionManager {

    @Inject(method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Ljava/util/Collection;)V", at = @At("HEAD"), remap = false)
    private void onUploadResults(CommandList commandList, Collection<BuilderTaskOutput> results, CallbackInfo ci) {
        if (!UniversalNvidiumMod.isActive()) return;
        if (!UniversalNvidiumMod.getArchitecture().supportsMeshShaders()) {
            for (BuilderTaskOutput output : results) {
                if (output != null && !output.isDisposed()) {
                    PascalRenderBackend.getInstance().onChunkUpload(output);
                }
            }
        }
    }

    @Inject(method = "uploadResults(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;)V", at = @("HEAD"), remap = false)
    private void onUploadResults(CommandList commandList, RenderRegion region, Collection<BuilderTaskOutput> results, CallbackInfo ci) {
        if (!UniversalNvidiumMod.isActive()) return;
        if (!UniversalNvidiumMod.getArchitecture().supportsMeshShaders()) {
            for (BuilderTaskOutput output : results) {
                if (output != null && !output.isDisposed()) {
                    PascalRenderBackend.getInstance().onChunkUpload(output);
                }
            }
        }
    }
}
