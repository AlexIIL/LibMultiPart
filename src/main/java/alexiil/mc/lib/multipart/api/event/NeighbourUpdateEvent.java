/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import net.minecraft.util.math.BlockPos;

/** Fired when a neighbouring block was updated. */
public class NeighbourUpdateEvent extends MultipartEvent {
    public final BlockPos pos;

    public NeighbourUpdateEvent(BlockPos pos) {
        this.pos = pos;
    }
}
