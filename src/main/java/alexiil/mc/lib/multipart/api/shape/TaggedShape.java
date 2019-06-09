package alexiil.mc.lib.multipart.api.shape;

import javax.annotation.Nullable;

import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import alexiil.mc.lib.multipart.api.MultiPartContainer;

/** A {@link VoxelShape} alongside an optional "tag", which is used to determine what can overlap with this shape in a
 * {@link MultiPartContainer}. */
public final class TaggedShape {
    public final VoxelShape shape;
    public final ShapeTag tag;

    public TaggedShape(VoxelShape shape) {
        this(shape, null);
    }

    public TaggedShape(VoxelShape shape, @Nullable ShapeTag tag) {
        this.shape = shape;
        this.tag = tag == null ? DefaultShapeTags.DEFAULT : tag;
    }

    public boolean doesInvalidlyOverlap(TaggedShape other) {
        if (!VoxelShapes.matchesAnywhere(shape, other.shape, BooleanBiFunction.AND)) {
            return false;
        }
        return !ShapeTag.canOverlap(tag, other.tag);
    }
}
