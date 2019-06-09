package alexiil.mc.lib.multipart.api.shape;

public enum DefaultShapeTags implements ShapeTag {
    /** The default {@link ShapeTag} that everything uses by default. This always collides with everything, and never
     * allows overlapping with anything else. */
    DEFAULT;

    @Override
    public boolean canOverlap(ShapeTag other) {
        return false;
    }
}
