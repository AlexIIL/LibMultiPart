/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/** A {@link BlockView} that is backed by a different {@link BlockView}, but returns a different {@link BlockState},
 * {@link FluidState}, and {@link BlockEntity} for a single {@link BlockPos}. */
public final class SingleReplacementBlockView implements BlockView {
    private final BlockView real;
    private final BlockPos replacedPos;
    private final BlockState replacedState;
    private final BlockEntity replacedEntity;

    public SingleReplacementBlockView(BlockView world, BlockPos replacedPos, BlockState state) {
        this.real = world;
        this.replacedPos = replacedPos;
        this.replacedState = state;
        this.replacedEntity = null;
    }

    public SingleReplacementBlockView(
        BlockView world, BlockPos replacedPos, BlockState state, BlockEntity blockEntity
    ) {
        this.real = world;
        this.replacedPos = replacedPos;
        this.replacedState = state;
        this.replacedEntity = blockEntity;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return pos.equals(replacedPos) ? replacedEntity : real.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return pos.equals(replacedPos) ? replacedState : real.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return pos.equals(replacedPos) ? replacedState.getFluidState() : real.getFluidState(pos);
    }
}
