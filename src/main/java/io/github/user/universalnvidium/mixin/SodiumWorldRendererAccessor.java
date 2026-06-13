package io.github.user.universalnvidium.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public interface SodiumWorldRendererAccessor {
    @Accessor("renderSectionManager")
    RenderSectionManager getRenderSectionManager();
}
