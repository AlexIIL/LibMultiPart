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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolemEntity.class)
public abstract class Client_IronGolemEntityMixin extends GolemEntity implements Angerable {

    protected Client_IronGolemEntityMixin(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"),
            method = "tickMovement()V", cancellable = true)
    private void tickMovment(CallbackInfo ci) {
        // The associated IronGolemEntity method seems to be called on both the client and the server, but the call to
        // `world.addParticle()` only does anything on the client.
        if (world.isClient) {
            BlockPos pos =
                    new BlockPos(MathHelper.floor(getX()), MathHelper.floor(getY() - 0.2), MathHelper.floor(getZ()));
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof IBlockCustomParticles) {
                if (((IBlockCustomParticles) block).spawnIronGolemParticles(world, pos, state,
                        (IronGolemEntity) (Object) this, getRandom())) {
                    ci.cancel();
                }
            }
        }
    }
}
