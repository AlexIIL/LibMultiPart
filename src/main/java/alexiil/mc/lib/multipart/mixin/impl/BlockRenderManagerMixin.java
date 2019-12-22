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

import net.minecraft.client.render.block.BlockRenderManager;

import alexiil.mc.lib.multipart.mixin.api.IBlockRenderManagerMixin;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin implements IBlockRenderManagerMixin {

    @Unique
    private boolean renderingPartiallyBrokenBlocks;

    @Override
    public boolean libmultipart_isRenderingPartiallyBrokenBlocks() {
        return renderingPartiallyBrokenBlocks;
    }

    @Inject(
        at = { @At("HEAD") },
        method = "renderDamage(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;"
            + "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V"
    )
    private void beginPartiallyBrokenBlocks(CallbackInfo ci) {
        renderingPartiallyBrokenBlocks = true;
    }

    @Inject(
        at = { @At("RETURN") },
        method = "renderDamage(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;"
            + "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V"
    )
    private void endPartiallyBrokenBlocks(CallbackInfo ci) {
        renderingPartiallyBrokenBlocks = false;
    }
}
