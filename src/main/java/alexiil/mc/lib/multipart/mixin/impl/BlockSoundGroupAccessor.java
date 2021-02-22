/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;

@Mixin(BlockSoundGroup.class)
public interface BlockSoundGroupAccessor {

    @Accessor("breakSound")
    SoundEvent libmultipart_getBreakSound();

    @Accessor("hitSound")
    SoundEvent libmultipart_getHitSound();
}
