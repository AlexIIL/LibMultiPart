/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import alexiil.mc.lib.multipart.mixin.api.IBlockDynamicCull;

@Mixin(Block.class)
public class Client_BlockMixin {

    @Inject(
        method = "Lnet/minecraft/block/Block;shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/util/math/Direction;)Z",
        cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOpaque()Z")
    )
    private static void shouldDrawSide(
        BlockState state, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> ci
    ) {
        BlockPos oPos = pos.offset(facing);
        BlockState oState = view.getBlockState(oPos);
        Block block = state.getBlock();
        Block oBlock = oState.getBlock();
        if (
            (block instanceof IBlockDynamicCull && ((IBlockDynamicCull) block).hasDynamicCull(state))
                || (oBlock instanceof IBlockDynamicCull && ((IBlockDynamicCull) oBlock).hasDynamicCull(oState))
        ) {
            VoxelShape voxelShape = state.getCullingFace(view, pos, facing);
            VoxelShape voxelShape2 = oState.getCullingFace(view, oPos, facing.getOpposite());
            ci.setReturnValue(VoxelShapes.matchesAnywhere(voxelShape, voxelShape2, BooleanBiFunction.ONLY_FIRST));
        }
    }
}
