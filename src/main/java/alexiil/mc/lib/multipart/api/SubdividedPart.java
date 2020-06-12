/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/** Optional interface for {@link AbstractPart} implementations which can have sub-parts targeted and broken instead of
 * the whole thing. */
public interface SubdividedPart<Sub> {

    Class<Sub> getSubpartKeyClass();

    /** Multipart version of {@link Block#onBlockBreakStart(BlockState, World, BlockPos, PlayerEntity)} */
    void onSubpartBreakStart(PlayerEntity player, Sub subpart);

    /** Multipart version of {@link Block#calcBlockBreakingDelta(BlockState, PlayerEntity, BlockView, BlockPos)} */
    float calcSubpartBreakingDelta(Sub subpart);

    /** Multipart version of {@link Block#onBreak(World, BlockPos, BlockState, PlayerEntity)}.
     * 
     * @return True if this should prevent Block.onBreak from being called afterwards, false otherwise. */
    default boolean onSubpartBreak(PlayerEntity player, Sub subpart) {
        return false;
    }

    /** Called instead of {@link World#removeBlock(BlockPos, boolean)} in
     * {@link ServerPlayerInteractionManager#tryBreakBlock}.
     * <p>
     * Generally this should remove the subpart from this {@link AbstractPart}, or return false if the whole part needs
     * to be removed from it's container.
     * 
     * @return True if this should prevent {@link MultipartContainer#removePart(AbstractPart)} from being called, false
     *         otherwise. */
    boolean clearSubpart(Sub subpart);

    /** Multipart version of {@link Block#onBroken(WorldAccess, BlockPos, BlockState)} */
    void onSubpartBroken(Sub subpart);

    /** Multipart version of
     * {@link Block#afterBreak(World, PlayerEntity, BlockPos, BlockState, BlockEntity, ItemStack)}. */
    void afterSubpartBreak(PlayerEntity player, ItemStack tool, DefaultedList<ItemStack> drops, Sub subpart);

    /** Subpart equivalent to {@link AbstractPart#getDynamicShape(float)}. If the given part is invalid then this should
     * return {@link AbstractPart#getDynamicShape(float)}. */
    VoxelShape getSubpartDynamicShape(Vec3d hitVec, Sub subpart, float partialTicks);

    default boolean spawnBreakingParticles(Vec3d hitVec, Sub subpart, Direction side) {
        return false;
    }

    /** @param hitVec The exact hit position, relative the the world's origin. (So you need to subtract the position of
     *            this part to get a position between 0 and 1).
     * @return The targeted subpart, or null if the given position doesn't intersect with a subpart. */
    @Nullable
    Sub getTargetedSubpart(Vec3d hitVec);
}
