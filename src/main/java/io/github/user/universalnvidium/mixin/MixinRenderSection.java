package io.github.user.universalnvidium.mixin;

import io.github.user.universalnvidium.UniversalNvidiumMod;
import io.github.user.universalnvidium.render.backend.PascalRenderBackend;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSection", remap = false)
public class MixinRenderSection {

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    private void onDelete(CallbackInfo ci) {
        if (!UniversalNvidiumMod.isActive()) return;
        if (!UniversalNvidiumMod.getArchitecture().supportsMeshShaders()) {
            PascalRenderBackend.getInstance().onSectionDelete((RenderSection) (Object) this);
        }
    }
}
