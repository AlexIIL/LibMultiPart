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
    private boolean drawingBlockOutline;

    @Override
    public boolean libmultipart_isDrawingBlockOutline() {
        return drawingBlockOutline;
    }

    @Inject(
        at = { @At("HEAD") },
        method = ("drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;"
            + "DDDLnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    )
    private void beginBlockOutline(CallbackInfo ci) {
        assert !drawingBlockOutline;
        drawingBlockOutline = true;
    }

    @Inject(
        at = { @At("RETURN") },
        method = ("drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;"
            + "DDDLnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V")
    )
    private void endBlockOutline(CallbackInfo ci) {
        assert drawingBlockOutline;
        drawingBlockOutline = false;
    }
}
