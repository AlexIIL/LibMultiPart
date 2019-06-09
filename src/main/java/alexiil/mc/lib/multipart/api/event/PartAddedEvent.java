package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultiPartContainer;

/** Fired whenever an {@link AbstractPart} is added to a {@link MultiPartContainer}. */
public final class PartAddedEvent extends MultiPartEvent {
    public final AbstractPart part;

    public PartAddedEvent(AbstractPart part) {
        this.part = part;
    }
}
