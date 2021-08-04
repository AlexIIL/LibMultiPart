/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

/** Fired once per {@linkplain net.minecraft.block.BlockState#scheduledTick(net.minecraft.server.world.ServerWorld, net.minecraft.util.math.BlockPos, java.util.Random)
 * scheduled tick.} */
public final class PartScheduledTickEvent extends MultipartEvent implements ContextlessEvent {
    public static final PartScheduledTickEvent INSTANCE = new PartScheduledTickEvent();

    private PartScheduledTickEvent() {}
}
