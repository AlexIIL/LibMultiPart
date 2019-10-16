/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import javax.annotation.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.MultipartContainer.PartOffer;
import alexiil.mc.lib.multipart.impl.MultipartUtilImpl;

/** Contains various utilities for creating, accessing, or interacting with {@link MultipartContainer}'s in a
 * {@link World}. */
public final class MultipartUtil {
    private MultipartUtil() {}

    /** Checks to see if the block at the given position currently contains a full {@link MultipartContainer}. This does
     * not take into account {@link NativeMultipart}. */
    @Nullable
    public static MultipartContainer get(World world, BlockPos pos) {
        return MultipartUtilImpl.get(world, pos);
    }

    /** Offers the given {@link AbstractPart} into the block at the given position. This may return a non-null
     * {@link PartOffer} if */
    @Nullable
    public static PartOffer offerNewPart(World world, BlockPos pos, MultipartCreator creator) {
        return offerNewPart(world, pos, creator, true);
    }

    /** Offers the given {@link AbstractPart} into the block at the given position. This may return a non-null
     * {@link PartOffer} if */
    @Nullable
    public static PartOffer offerNewPart(World world, BlockPos pos, MultipartCreator creator, boolean respectEntityBBs) {
        return MultipartUtilImpl.offerNewPart(world, pos, creator, respectEntityBBs);
    }

    /** Turns an existing {@link NativeMultipart} block into a {@link BlockEntity} based {@link MultipartContainer}.
     * This is only useful for updating old blocks, as adding new parts can be done via
     * {@link #offerNewPart(World, BlockPos, MultipartCreator)}. */
    @Nullable
    public static MultipartContainer turnIntoMultipart(World world, BlockPos pos) {
        return MultipartUtilImpl.turnIntoMultipart(world, pos);
    }
}
