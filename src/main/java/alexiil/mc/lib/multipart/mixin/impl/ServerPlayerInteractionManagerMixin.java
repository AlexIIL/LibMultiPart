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
        if (!field_14003) {
            multipartKey = null;
        }
    }

    @Redirect(
        method = "method_14263(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;onBlockBreakStart(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V"))
    void onBlockBreakStart(BlockState state, World w, BlockPos pos, PlayerEntity pl) {
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
            // Guess the hit vec from the player's look vector
            VoxelShape shape = state.getCollisionShape(w, pos);
            BlockHitResult rayTrace = shape.rayTrace(
                pl.getCameraPosVec(1), pl.getCameraPosVec(1).add(pl.getRotationVec(1).multiply(10)), pos
            );
            if (rayTrace == null) {
                // This shouldn't really happen... lets just fail.
                return;
            }
            vec = rayTrace.getPos();
        }
        T bestKey = block.getTargetedMultipart(state, w, pos, pl, vec);

        if (bestKey == null) {
            // TODO!
        } else {
            this.multipartKey = bestKey;
            block.onBlockBreakStart(state, w, pos, pl, bestKey);
        }
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        BlockState state = world.getBlockState(pos);

        if (!field_14003) {
            // Being broken by a creative player
            onBlockBreakStart(state, world, pos, player);
        }

        Block block = state.getBlock();
        if (block instanceof IBlockMultipart<?>) {
            boolean ret = destroyBlock0(pos, state, (IBlockMultipart<?>) block);
            ci.setReturnValue(ret);
        }
    }

    private <T> boolean destroyBlock0(BlockPos pos, BlockState state, IBlockMultipart<T> block) {
        if (multipartKey == null) {
            return false;
        }
        if (!block.getKeyClass().isInstance(multipartKey)) {
            return false;
        }
        T key = block.getKeyClass().cast(multipartKey);
        block.onBreak(world, pos, state, player, key);
        if (block.clearBlockState(world, pos, key)) {
            block.onBroken(world, pos, state, key);
            return true;
        } else {
            return false;
        }
    }

    @Redirect(
        method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;"
            + "Lnet/minecraft/item/ItemStack;)V"))
    void afterBreak(Block block, World w, PlayerEntity pl, BlockPos pos, BlockState state, BlockEntity be,
        ItemStack stack) {
        if (
            !(block instanceof IBlockMultipart<?>)
            || !afterBreak0((IBlockMultipart<?>) block, w, pl, pos, state, be, stack)
        ) {
            block.afterBreak(w, pl, pos, state, be, stack);
        }
    }

    private <T> boolean afterBreak0(IBlockMultipart<T> block, World w, PlayerEntity pl, BlockPos pos, BlockState state,
        BlockEntity be, ItemStack stack) {
        if (multipartKey == null) {
            return false;
        }
        if (!block.getKeyClass().isInstance(multipartKey)) {
            return false;
        }
        block.afterBreak(w, pl, pos, state, be, stack, block.getKeyClass().cast(multipartKey));
        return true;
    }
}
