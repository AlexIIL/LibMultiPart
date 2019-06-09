package alexiil.mc.lib.multipart.mixin.impl;

import java.util.Objects;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;
import alexiil.mc.lib.multipart.mixin.api.IClientPlayerInteractionManagerMixin;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManagerMixin {

    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    BlockPos currentBreakingPos;

    @Unique
    Object partKey;

    @Override
    public BlockPos libmultipart_getCurrentBreakPosition() {
        return currentBreakingPos;
    }

    @Override
    public Object libmultipart_getPartKey() {
        return partKey;
    }

    @Redirect(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;onBlockBreakStart(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V"),
        method = "attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z")
    void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        Block block = state.getBlock();
        if (block instanceof IBlockMultipart<?>) {
            IBlockMultipart<?> blockMulti = (IBlockMultipart<?>) block;
            onBlockBreakStart0(state, world, pos, player, blockMulti);
        } else {
            state.onBlockBreakStart(world, pos, player);
        }
    }

    private <T> void onBlockBreakStart0(BlockState state, World world, BlockPos pos, PlayerEntity player,
        IBlockMultipart<T> blockMulti) {

        HitResult hit = MinecraftClient.getInstance().hitResult;
        T key = blockMulti.getTargetedMultipart(state, world, pos, player, hit.getPos());
        partKey = key;
        blockMulti.onBlockBreakStart(state, world, pos, player, key);
    }

    @Inject(
        method = "breakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V"),
        cancellable = true)
    void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        World world = client.world;
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof IBlockMultipart<?>) {
            IBlockMultipart<?> blockMulti = (IBlockMultipart<?>) state.getBlock();
            Boolean ret = breakBlock0(pos, blockMulti);
            if (ret != null) {
                ci.setReturnValue(ret);
                return;
            }
        }
    }

    private <T> Boolean breakBlock0(BlockPos pos, IBlockMultipart<T> blockMulti) {

        HitResult hit = MinecraftClient.getInstance().hitResult;
        World world = client.world;
        BlockState state = world.getBlockState(pos);
        T target = blockMulti.getTargetedMultipart(state, world, pos, client.player, hit.getPos());
        T previous;
        if (partKey == null) {
            previous = target;
        } else if (blockMulti.getKeyClass().isInstance(partKey)) {
            previous = blockMulti.getKeyClass().cast(partKey);
        } else {
            previous = target;
        }
        partKey = null;
        if (target == null || !Objects.equals(previous, target)) {
            currentBreakingPos = new BlockPos(currentBreakingPos.getX(), -1, currentBreakingPos.getZ());
            return Boolean.FALSE;
        }
        blockMulti.onBreak(world, pos, state, client.player, target);
        if (blockMulti.clearBlockState(world, pos, target)) {
            blockMulti.onBroken(world, pos, state, target);
            currentBreakingPos = new BlockPos(currentBreakingPos.getX(), -1, currentBreakingPos.getZ());
            return Boolean.TRUE;
        } else {
            currentBreakingPos = new BlockPos(currentBreakingPos.getX(), -1, currentBreakingPos.getZ());
            return Boolean.FALSE;
        }
    }
}
