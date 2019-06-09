package alexiil.mc.lib.multipart.api.event;

import java.util.function.Function;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultiPartContainer;

/** Fired whenever an {@link AbstractPart} is {@link MultiPartContainer#offerNewPart(Function) offered} or
 * {@link MultiPartContainer#addNewPart(Function) added} to a {@link MultiPartContainer}. */
public final class PartOfferedEvent extends MultiPartEvent {
    public final AbstractPart part;
    private boolean isAllowed = true;

    public PartOfferedEvent(AbstractPart part) {
        this.part = part;
    }

    public boolean isAllowed() {
        return isAllowed;
    }

    /** Disallows this part from being added to the container. */
    public void disallow() {
        isAllowed = false;
    }
}
