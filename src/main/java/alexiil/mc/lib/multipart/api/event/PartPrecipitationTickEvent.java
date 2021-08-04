/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import net.minecraft.world.biome.Biome;

/** Fired once per {@linkplain net.minecraft.block.Block#precipitationTick(net.minecraft.block.BlockState, net.minecraft.world.World, net.minecraft.util.math.BlockPos, net.minecraft.world.biome.Biome.Precipitation)
 * precipitation tick.} */
public final class PartPrecipitationTickEvent extends MultipartEvent {
    public final Biome.Precipitation precipitation;

    public PartPrecipitationTickEvent(Biome.Precipitation precipitation) {
        this.precipitation = precipitation;
    }
}
