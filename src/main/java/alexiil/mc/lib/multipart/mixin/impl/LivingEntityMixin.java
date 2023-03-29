/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import alexiil.mc.lib.multipart.mixin.api.IBlockCustomParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;spawnParticles(Lnet/minecraft/particle/ParticleEffect;DDDIDDDD)I"),
            method = "fall(DZLnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V", cancellable = true)
    private void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition,
                      CallbackInfo ci) {
        BlockPos blockPos = BlockPos.ofFloored(getX(), getY() - 0.2, getZ());
        BlockState state = world.getBlockState(blockPos);
        Block block = state.getBlock();
        if (block instanceof IBlockCustomParticles) {
            // This injector injects into an if statement that checks `!world.isClient`
            if (((IBlockCustomParticles) block).spawnFallParticles((ServerWorld) world, blockPos, state,
                    (LivingEntity) (Object) this, random)) {
                ci.cancel();

                // Since we're canceling, the rest of this method won't run. We should do the things that would normally
                // come after.
                super.fall(heightDifference, onGround, landedState, landedPosition);
            }
        }
    }
}
