package alexiil.mc.lib.multipart.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.WorldRenderer;

import alexiil.mc.lib.multipart.mixin.api.IWorldRendererMixin;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements IWorldRendererMixin {

    @Unique
    private boolean renderingPartiallyBrokenBlocks;

    @Override
    public boolean libmultipart_isRenderingPartiallyBrokenBlocks() {
        return renderingPartiallyBrokenBlocks;
    }

    @Inject(at = { @At("HEAD") }, method = "enableBlockOverlayRendering()V")
    private void beginPartiallyBrokenBlocks(CallbackInfo ci) {
        renderingPartiallyBrokenBlocks = true;
    }

    @Inject(at = { @At("HEAD") }, method = "disableBlockOverlayRendering()V")
    private void endPartiallyBrokenBlocks(CallbackInfo ci) {
        renderingPartiallyBrokenBlocks = false;
    }
}
