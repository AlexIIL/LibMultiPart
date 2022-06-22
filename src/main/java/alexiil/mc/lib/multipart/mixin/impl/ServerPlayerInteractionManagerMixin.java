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
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.impl.LibMultiPart;
import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    // TODO: Render the correct part outline!
    // TODO: Render the correct damage model!

    @Shadow
    ServerWorld world;

    @Shadow
    ServerPlayerEntity player;

    @Shadow
    boolean mining;

    @Unique
    Object multipartKey;

    /** Sent from the client by a custom break packet (as a replacement for the normal "on block start break"
     * packet). */
    @Unique
    Vec3d sentHitVec;

    private void log(String text) {
        LibMultiPart.LOGGER.info("[player-interaction] '" + player.getEntityName() + "' " + text);
    }

    @Inject(method = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;update()V", at = @At("RETURN"))
    void update(CallbackInfo ci) {
        if (!mining && multipartKey != null) {
            if (LibMultiPart.DEBUG) {
                log("update(): Cleared multipartKey");
            }
            multipartKey = null;
        }
    }

    @Redirect(
        method = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;processBlockBreakingAction("
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;"
            + "Lnet/minecraft/util/math/Direction;II)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;onBlockBreakStart(Lnet/minecraft/world/World;"
                + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V"))
    void onBlockBreakStart(BlockState state, World w, BlockPos pos, PlayerEntity pl) {
        if (LibMultiPart.DEBUG) {
            log("onBlockBreakStart( " + pos + " " + state + " )");
        }
        if (state.getBlock() instanceof IBlockMultipart<?>) {
            IBlockMultipart<?> block = (IBlockMultipart<?>) state.getBlock();
            onBlockBreakStart0(state, w, pos, pl, block);
        } else {
            state.onBlockBreakStart(w, pos, pl);
        }
    }

    private <T> void onBlockBreakStart0(
        BlockState state, World w, BlockPos pos, PlayerEntity pl, IBlockMultipart<T> block
    ) {

        Vec3d vec = sentHitVec;
        if (vec == null) {
            if (LibMultiPart.DEBUG) {
                log("onBlockBreakStart0(): vec was null!");
            }
            // Guess the hit vec from the player's look vector
            VoxelShape shape = state.getOutlineShape(w, pos);
            BlockHitResult rayTrace = shape
                .raycast(pl.getCameraPosVec(1), pl.getCameraPosVec(1).add(pl.getRotationVec(1).multiply(10)), pos);
            if (LibMultiPart.DEBUG) {
                log("onBlockBreakStart(): rayTrace = " + rayTrace);
            }
            if (rayTrace == null) {
                // This shouldn't really happen... lets just fail.
                return;
            }
            vec = rayTrace.getPos();
        }
        if (LibMultiPart.DEBUG) {
            log("onBlockBreakStart(): vec = " + vec);
        }
        T bestKey = block.getTargetedMultipart(state, w, pos, vec);
        if (LibMultiPart.DEBUG) {
            log("onBlockBreakStart(): bestKey = " + bestKey);
        }

        if (bestKey == null) {
            // TODO!
        } else {
            this.multipartKey = bestKey;
            block.onBlockBreakStart(state, w, pos, pl, bestKey);
        }
    }

    @Redirect(at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;"
            + "Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"),
        method = "*")
    float calcBlockBreakingDelta(BlockState state, PlayerEntity pl, BlockView view, BlockPos pos) {
        if (LibMultiPart.DEBUG) {
            log("calcBlockBreakingDelta( " + pos + " " + state + " )");
        }
        if (state.getBlock() instanceof IBlockMultipart<?>) {
            IBlockMultipart<?> block = (IBlockMultipart<?>) state.getBlock();
            return calcBlockBreakingDelta0(block, state, pl, view, pos);
        } else {
            return state.calcBlockBreakingDelta(pl, view, pos);
        }
    }

    private <T> float calcBlockBreakingDelta0(
        IBlockMultipart<T> block, BlockState state, PlayerEntity pl, BlockView view, BlockPos pos
    ) {
        if (multipartKey == null) {
            // We haven't actually started breaking yet?
            onBlockBreakStart(state, world, pos, player);
        }

        if (multipartKey == null) {
            return state.calcBlockBreakingDelta(pl, view, pos);
        }

        if (!block.getKeyClass().isInstance(multipartKey)) {
            if (LibMultiPart.DEBUG) {
                log(
                    "calcBlockBreakingDelta0(): Wrong key " + multipartKey.getClass() + ", expected "
                        + block.getKeyClass()
                );
            }
            return state.calcBlockBreakingDelta(pl, view, pos);
        }

        T key = block.getKeyClass().cast(multipartKey);
        return block.calcBlockBreakingDelta(state, pl, view, pos, key);
    }

    @Inject(
        method = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V"),
        cancellable = true)
    void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        if (LibMultiPart.DEBUG) {
            log("destroyBlock( " + pos + " )");
        }
        BlockState state = world.getBlockState(pos);
        if (LibMultiPart.DEBUG) {
            log("destroyBlock(): multipartKey = " + multipartKey);
        }
        if (multipartKey == null) {
            // We haven't actually started breaking yet?
            onBlockBreakStart(state, world, pos, player);
        }

        Block block = state.getBlock();
        if (block instanceof IBlockMultipart<?>) {
            if (LibMultiPart.DEBUG) {
                log("destroyBlock(): multipartKey = " + multipartKey);
            }
            boolean ret = destroyBlock0(pos, state, (IBlockMultipart<?>) block);
            ci.setReturnValue(ret);
        }
    }

    private <T> boolean destroyBlock0(BlockPos pos, BlockState state, IBlockMultipart<T> block) {
        if (multipartKey == null) {
            return false;
        }
        if (!block.getKeyClass().isInstance(multipartKey)) {
            if (LibMultiPart.DEBUG) {
                log("destroyBlock0(): Wrong key " + multipartKey.getClass() + ", expected " + block.getKeyClass());
            }
            return false;
        }
        T key = block.getKeyClass().cast(multipartKey);
        BlockEntity be = world.getBlockEntity(pos);
        block.onBreak(world, pos, state, player, key);
        if (block.clearBlockState(world, pos, key)) {
            if (LibMultiPart.DEBUG) {
                log("destroyBlock0(): IBlockMultipart.clearBlockState() -> true");
            }
            block.onBroken(world, pos, state, key);
            if (!player.isCreative()) {
                ItemStack stack = player.getMainHandStack();
                stack.postMine(world, state, pos, player);
                block.afterBreak(world, player, pos, state, be, stack, key);
            }
            return true;
        } else {
            if (LibMultiPart.DEBUG) {
                log("destroyBlock0(): IBlockMultipart.clearBlockState() -> false");
            }
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
