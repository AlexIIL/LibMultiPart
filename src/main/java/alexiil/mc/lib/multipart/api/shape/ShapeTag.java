package alexiil.mc.lib.multipart.api.shape;

import alexiil.mc.lib.multipart.api.AbstractPart;

/** Used in {@link AbstractPart#getOverlapShapeInformation()} to provide more information about how a part is allowed to
 * overlap with another part. The default value is always {@link DefaultShapeTags#DEFAULT}, which never allows
 * overlapping with anything. */
public interface ShapeTag {
    boolean canOverlap(ShapeTag other);

    static boolean canOverlap(ShapeTag a, ShapeTag b) {
        return a.canOverlap(b) && b.canOverlap(a);
    }
}
