package alexiil.mc.lib.multipart.api;

/** Wrapper interface for an {@link AbstractPart} in a {@link MultiPartContainer}. */
public interface MultiPartHolder {

    MultiPartContainer getContainer();

    AbstractPart getPart();

    /** Removes this {@link #getPart()} from the container. */
    void remove();
}
