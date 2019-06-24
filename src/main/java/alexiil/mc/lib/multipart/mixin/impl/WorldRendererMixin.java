/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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

    @Unique
    private boolean drawingBlockOutline;

    @Override
    public boolean libmultipart_isRenderingPartiallyBrokenBlocks() {
        return renderingPartiallyBrokenBlocks;
    }

    @Override
    public boolean libmultipart_isDrawingBlockOutline() {
        return drawingBlockOutline;
    }

    @Inject(at = { @At("HEAD") }, method = "enableBlockOverlayRendering()V")
    private void beginPartiallyBrokenBlocks(CallbackInfo ci) {
        renderingPartiallyBrokenBlocks = true;
    }

    @Inject(at = { @At("HEAD") }, method = "disableBlockOverlayRendering()V")
    private void endPartiallyBrokenBlocks(CallbackInfo ci) {
        renderingPartiallyBrokenBlocks = false;
    }

    @Inject(
        at = { @At("HEAD") },
        method = ("drawHighlightedBlockOutline(Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/hit/HitResult;I)V"))
    private void beginBlockOutline(CallbackInfo ci) {
        assert !drawingBlockOutline;
        drawingBlockOutline = true;
    }

    @Inject(
        at = { @At("RETURN") },
        method = ("drawHighlightedBlockOutline(Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/hit/HitResult;I)V"))
    private void endBlockOutline(CallbackInfo ci) {
        assert drawingBlockOutline;
        drawingBlockOutline = false;
    }
}
