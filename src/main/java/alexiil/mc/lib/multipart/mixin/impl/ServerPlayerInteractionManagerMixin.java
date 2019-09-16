/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    // TODO: Render the correct part outline!
    // TODO: Render the correct damage model!

    @Shadow
    ServerWorld world;

    @Shadow
    ServerPlayerEntity player;

    /** isBreaking (?) */
    @Shadow
    boolean field_14003;

    @Unique
    Object multipartKey;

    /** Sent from the client by a custom break packet (as a replacement for the normal "on block start break"
     * packet). */
    @Unique
    Vec3d sentHitVec;

    @Inject(method = "update()V", at = @At("RETURN"))
    void update(CallbackInfo ci) {
        if (!field_14003 && multipartKey != null) {
            // LibMultiPart.LOGGER.info("[server] Cleared multipartKey");
            multipartKey = null;
        }
    }

    @Redirect(
        method = "method_14263(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/server/network/packet/PlayerActionC2SPacket/Action;"
            + "Lnet/minecraft/util/math/Direction;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;onBlockBreakStart(Lnet/minecraft/world/World;"
                + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V"))
    void onBlockBreakStart(BlockState state, World w, BlockPos pos, PlayerEntity pl) {
        // LibMultiPart.LOGGER.info("[server] onBlockBreakStart( " + pos + " " + state + " )");
        if (state.getBlock() instanceof IBlockMultipart<?>) {
            IBlockMultipart<?> block = (IBlockMultipart<?>) state.getBlock();
            onBlockBreakStart0(state, w, pos, pl, block);
        } else {
            state.onBlockBreakStart(w, pos, pl);
        }
    }

    private <T> void onBlockBreakStart0(BlockState state, World w, BlockPos pos, PlayerEntity pl, IBlockMultipart<
        T> block) {

        Vec3d vec = sentHitVec;
        if (vec == null) {
            // LibMultiPart.LOGGER.info("[server] vec was null!");
            // Guess the hit vec from the player's look vector
            VoxelShape shape = state.getOutlineShape(w, pos);
            BlockHitResult rayTrace = shape.rayTrace(
                pl.getCameraPosVec(1), pl.getCameraPosVec(1).add(pl.getRotationVec(1).multiply(10)), pos
            );
            // LibMultiPart.LOGGER.info("[server] rayTrace = " + rayTrace);
            if (rayTrace == null) {
                // This shouldn't really happen... lets just fail.
                return;
            }
            vec = rayTrace.getPos();
        }
        // LibMultiPart.LOGGER.info("[server] vec = " + vec);
        T bestKey = block.getTargetedMultipart(state, w, pos, vec);
        // LibMultiPart.LOGGER.info("[server] bestKey = " + bestKey);

        if (bestKey == null) {
            // TODO!
        } else {
            this.multipartKey = bestKey;
            block.onBlockBreakStart(state, w, pos, pl, bestKey);
        }
    }

    @Inject(
        method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;"
                + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V"),
        cancellable = true)
    void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        // LibMultiPart.LOGGER.info("[server] destroyBlock( " + pos + " )");
        BlockState state = world.getBlockState(pos);
        // LibMultiPart.LOGGER.info("[server] multipartKey = " + multipartKey);
        if (multipartKey == null) {
            // We haven't actually started breaking yet?
            onBlockBreakStart(state, world, pos, player);
        }

        Block block = state.getBlock();
        if (block instanceof IBlockMultipart<?>) {
            // LibMultiPart.LOGGER.info("[server] multipartKey = " + multipartKey);
            boolean ret = destroyBlock0(pos, state, (IBlockMultipart<?>) block);
            ci.setReturnValue(ret);
        }
    }

    private <T> boolean destroyBlock0(BlockPos pos, BlockState state, IBlockMultipart<T> block) {
        if (multipartKey == null) {
            return false;
        }
        if (!block.getKeyClass().isInstance(multipartKey)) {
            // LibMultiPart.LOGGER.info("[server] wrong key class");
            return false;
        }
        T key = block.getKeyClass().cast(multipartKey);
        BlockEntity be = world.getBlockEntity(pos);
        block.onBreak(world, pos, state, player, key);
        if (block.clearBlockState(world, pos, key)) {
            // LibMultiPart.LOGGER.info("[server] was cleared!");
            block.onBroken(world, pos, state, key);
            if (!player.isCreative()) {
                ItemStack stack = player.getMainHandStack();
                stack.postMine(world, state, pos, player);
                block.afterBreak(world, player, pos, state, be, stack, key);
            }
            return true;
        } else {
            // LibMultiPart.LOGGER.info("[server] wasn't cleared!");
            return false;
        }
    }

    // @Redirect(
    // method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
    // at = @At(
    // value = "INVOKE",
    // target =
    // "Lnet/minecraft/block/Block;afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;"
    // + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;"
    // + "Lnet/minecraft/item/ItemStack;)V"))
    // void afterBreak(Block block, World w, PlayerEntity pl, BlockPos pos, BlockState state, BlockEntity be,
    // ItemStack stack) {
    // if (
    // !(block instanceof IBlockMultipart<?>) || !afterBreak0(
    // (IBlockMultipart<?>) block, w, pl, pos, state, be, stack
    // )
    // ) {
    // block.afterBreak(w, pl, pos, state, be, stack);
    // }
    // }

    // private <T> boolean afterBreak0(IBlockMultipart<T> block, World w, PlayerEntity pl, BlockPos pos, BlockState
    // state,
    // BlockEntity be, ItemStack stack) {
    // if (multipartKey == null) {
    // return false;
    // }
    // if (!block.getKeyClass().isInstance(multipartKey)) {
    // return false;
    // }
    // block.afterBreak(w, pl, pos, state, be, stack, block.getKeyClass().cast(multipartKey));
    // return true;
    // }
}
