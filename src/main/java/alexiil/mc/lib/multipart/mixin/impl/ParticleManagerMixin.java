/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import alexiil.mc.lib.multipart.mixin.api.IBlockCustomParticles;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(at = @At("HEAD"),
        method = "addBlockBreakingParticles(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)V",
        cancellable = true)
    private void addBlockBreakingParticles(BlockPos pos, Direction side, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null) {
            return;
        }
        BlockState state = w.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof IBlockCustomParticles) {
            IBlockCustomParticles pb = (IBlockCustomParticles) block;
            HitResult target = mc.crosshairTarget;
            Vec3d hitVec = target != null ? target.getPos() : Vec3d.ofCenter(pos);
            if (pb.spawnBreakingParticles(w, pos, state, side, hitVec)) {
                ci.cancel();
            }
        }
    }
}
