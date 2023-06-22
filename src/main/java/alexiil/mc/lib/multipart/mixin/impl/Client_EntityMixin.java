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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class Client_EntityMixin {

    @Shadow
    private World world;

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    @Final
    protected Random random;

    @Inject(at = @At("HEAD"), method = "spawnSprintingParticles()V", cancellable = true)
    private void spawnSprintingParticles(CallbackInfo ci) {
        // The associated Entity method seems to be called on both the client and the server, but the call to
        // `world.addParticle()` only does anything on the client.
        if (world.isClient) {
            BlockPos blockPos = BlockPos.ofFloored(getX(), getY() - 0.2, getZ());
            BlockState state = world.getBlockState(blockPos);
            Block block = state.getBlock();
            if (block instanceof IBlockCustomParticles) {
                if (((IBlockCustomParticles) block).spawnSprintingParticles(world, blockPos, state,
                        (Entity) (Object) this,
                        random)) {
                    ci.cancel();
                }
            }
        }
    }
}
