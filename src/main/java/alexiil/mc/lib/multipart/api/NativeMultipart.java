/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.impl.LibMultiPart;

/** A {@link Block} that can be converted (in-place) to an {@link AbstractPart}. (Usually called in
 * {@link MultipartUtil#offerNewPart(World, BlockPos, MultipartCreator)}. If you want to add support for other blocks
 * then you can use {@link #LOOKUP}.
 * <p>
 * This is generally only useful if your block does <strong>NOT</strong> already have a {@link BlockEntity}. */
public interface NativeMultipart {

    /** Only used after checking if a target {@link Block} implements {@link NativeMultipart}. */
    public static final BlockApiLookup<NativeMultipart, Void> LOOKUP
        = BlockApiLookup.get(LibMultiPart.id("native_multipart"), NativeMultipart.class, Void.class);

    /** @return A List of {@link MultipartCreator}'s that can create a new {@link AbstractPart} based on the current
     *         state at the given position, or null if the current state cannot be converted into an
     *         {@link AbstractPart}. An empty list will let this block be replaced with multiparts.
     *         <p>
     *         Note that neither this function call, nor the {@link Function} that this returns should modify the
     *         {@link World} in any way! */
    @Nullable
    List<MultipartCreator> getMultipartConversion(World world, BlockPos pos, BlockState state);
}
