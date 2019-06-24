package alexiil.mc.lib.multipart.api.event;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.mixin.api.IUnloadableBlockEntity;

/** Fired when the state of a container's block entity changes. Listen for subclasses, not this one! */
public abstract class PartContainerState extends MultiPartEvent {
    /** Fired in {@link BlockEntity#invalidate()} */
    public static final Invalidate INVALIDATE = new Invalidate();

    /** Fired in {@link BlockEntity#validate()} */
    public static final Validate VALIDATE = new Validate();

    /** Fired in {@link IUnloadableBlockEntity#onChunkUnload()} */
    public static final ChunkUnload CHUNK_UNLOAD = new ChunkUnload();

    /** Fired in {@link Block#onBlockRemoved(BlockState, World, BlockPos, BlockState, boolean)} */
    public static final Remove REMOVE = new Remove();

    PartContainerState() {}

    /** Fired in {@link BlockEntity#invalidate()} */
    public static final class Invalidate extends PartContainerState {
        Invalidate() {}
    }

    /** Fired in {@link BlockEntity#validate()} */
    public static final class Validate extends PartContainerState {
        Validate() {}
    }

    public static final class ChunkUnload extends PartContainerState {
        ChunkUnload() {}
    }

    /** Fired in {@link Block#onBlockRemoved(BlockState, World, BlockPos, BlockState, boolean)} */
    public static final class Remove extends PartContainerState {
        Remove() {}
    }
}
