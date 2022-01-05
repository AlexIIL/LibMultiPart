package alexiil.mc.lib.multipart.api;

import alexiil.mc.lib.multipart.impl.MissingPartImpl;

/** A special kind of {@link AbstractPart} which is used whenever a part cannot be read from NBT. */
public abstract class MissingPart extends AbstractPart {
    protected MissingPart(MissingPartDefinition definition, MultipartHolder holder) {
        super(definition, holder);
        if (getClass() != MissingPartImpl.class) {
            throw new IllegalStateException("Only LMP's implementation of MissingPart is permitted!");
        }
    }
}
