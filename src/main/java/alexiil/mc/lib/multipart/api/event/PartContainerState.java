/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.mixin.api.UnloadableBlockEntity;

/** Fired when the state of a container's block entity changes. Listen for subclasses, not this one! */
public abstract class PartContainerState extends MultipartEvent {
    /** Fired in {@link BlockEntity#markRemoved()} */
    public static final Invalidate INVALIDATE = new Invalidate();

    /** Fired in {@link BlockEntity#cancelRemoval()} */
    public static final Validate VALIDATE = new Validate();

    /** Fired in {@link UnloadableBlockEntity#onChunkUnload()}. In 1.17 this is fired before
     * {@link PartContainerState.Invalidate}, which always follows this. */
    public static final ChunkUnload CHUNK_UNLOAD = new ChunkUnload();

    /** Fired in {@link Block#onStateReplaced(BlockState, World, BlockPos, BlockState, boolean)} */
    public static final Remove REMOVE = new Remove();

    PartContainerState() {}

    /** Fired in {@link BlockEntity#markRemoved()} */
    public static final class Invalidate extends PartContainerState implements ContextlessEvent {
        Invalidate() {}
    }

    /** Fired in {@link BlockEntity#cancelRemoval()} */
    public static final class Validate extends PartContainerState implements ContextlessEvent {
        Validate() {}
    }

    /** Fired in {@link UnloadableBlockEntity#onChunkUnload()}. In 1.17 this is fired before
     * {@link PartContainerState.Invalidate}, which always follows this. */
    public static final class ChunkUnload extends PartContainerState implements ContextlessEvent {
        ChunkUnload() {}
    }

    /** Fired in {@link Block#onStateReplaced(BlockState, World, BlockPos, BlockState, boolean)} */
    public static final class Remove extends PartContainerState implements ContextlessEvent {
        Remove() {}
    }
}
