/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

/** Fired when a neighbouring block was updated in
 * {@link Block#getStateForNeighborUpdate(BlockState, Direction, BlockState, WorldAccess, BlockPos, BlockPos)}. */
public class NeighbourStateUpdateEvent extends MultipartEvent {
    public final Direction direction;
    public final BlockPos pos;
    public final BlockState newState;

    public NeighbourStateUpdateEvent(Direction direction, BlockPos pos, BlockState newState) {
        this.direction = direction;
        this.pos = pos;
        this.newState = newState;
    }
}
