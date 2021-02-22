/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.api;

import javax.annotation.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/** Interface for blocks to implement if parts of them can be targeted and broken independently from other parts. */
public interface IBlockMultipart<T> {
    Class<T> getKeyClass();

    /** Multipart version of {@link Block#onBlockBreakStart(BlockState, World, BlockPos, PlayerEntity)} */
    void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player, T subpart);

    /** Multipart version of {@link Block#calcBlockBreakingDelta(BlockState, PlayerEntity, BlockView, BlockPos)} */
    float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView view, BlockPos pos, T subpart);

    /** Multipart version of {@link Block#onBreak(World, BlockPos, BlockState, PlayerEntity)} */
    void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, T subpart);

    /** Called instead of {@link World#removeBlock(BlockPos, boolean)} in
     * {@link ServerPlayerInteractionManager#tryBreakBlock} */
    boolean clearBlockState(World world, BlockPos pos, T subpart);

    @Environment(EnvType.CLIENT)
    default boolean playHitSound(World world, BlockPos pos, BlockState state, PlayerEntity player, T subpart) {
        return false;
    }

    /** Multipart version of {@link Block#onBroken(WorldAccess, BlockPos, BlockState)} */
    void onBroken(WorldAccess world, BlockPos pos, BlockState state, T subpart);

    /** Multipart version of
     * {@link Block#afterBreak(World, PlayerEntity, BlockPos, BlockState, BlockEntity, ItemStack)}. */
    void afterBreak(
        World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity be, ItemStack stack,
        T subpart
    );

    /** @deprecated Please use {@link #getPartOutline(BlockState, BlockView, BlockPos, Vec3d)} instead. */
    @Deprecated
    VoxelShape getPartOutlineShape(BlockState state, World world, BlockPos pos, Vec3d hitVec);

    default VoxelShape getPartOutline(BlockState state, BlockView view, BlockPos pos, Vec3d hitVec) {
        if (view instanceof World) {
            return getPartOutlineShape(state, (World) view, pos, hitVec);
        } else {
            return VoxelShapes.empty();
        }
    }

    /** @deprecated Please use {@link #getMultipartTarget(BlockState, BlockView, BlockPos, Vec3d)} instead. */
    @Nullable
    @Deprecated
    T getTargetedMultipart(BlockState state, World world, BlockPos pos, Vec3d hitVec);

    @Nullable
    default T getMultipartTarget(BlockState state, BlockView view, BlockPos pos, Vec3d vec) {
        if (view instanceof World) {
            return getTargetedMultipart(state, (World) view, pos, vec);
        } else {
            return null;
        }
    }
}
