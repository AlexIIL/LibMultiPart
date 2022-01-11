/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

public interface IBlockCustomParticles {

    @Environment(EnvType.CLIENT)
    boolean spawnBreakingParticles(World world, BlockPos pos, BlockState state, Direction side, Vec3d hitVec);

    @Environment(EnvType.CLIENT)
    boolean spawnSprintingParticles(World world, BlockPos pos, BlockState state, Entity sprintingEntity, Random entityRandom);

    @Environment(EnvType.CLIENT)
    boolean spawnIronGolemParticles(World world, BlockPos pos, BlockState state, IronGolemEntity ironGolem, Random entityRandom);

    boolean spawnFallParticles(ServerWorld world, BlockPos pos, BlockState state, LivingEntity fallenEntity, Random entityRandom);
}
