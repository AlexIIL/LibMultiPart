package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.AbstractPart;

public final class PartRemovedEvent extends MultiPartEvent {
    public final AbstractPart removed;

    public PartRemovedEvent(AbstractPart removed) {
        this.removed = removed;
    }
}
