/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import net.minecraft.util.math.random.Random;

/** Fired once per
 * {@linkplain net.minecraft.block.Block#randomDisplayTick(net.minecraft.block.BlockState, net.minecraft.world.World, net.minecraft.util.math.BlockPos, Random)
 * random display tick.} */
public final class PartRandomDisplayTickEvent extends MultipartEvent implements ContextlessEvent {
    public static final PartRandomDisplayTickEvent INSTANCE = new PartRandomDisplayTickEvent();

    private PartRandomDisplayTickEvent() {}
}
