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
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Fired in {@link Block#onEntityCollision(BlockState, World, BlockPos, Entity)}. */
public final class PartEventEntityCollide extends MultipartEvent {
    public final Entity entity;

    public PartEventEntityCollide(Entity entity) {
        this.entity = entity;
    }
}
