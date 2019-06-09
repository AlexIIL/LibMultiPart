package alexiil.mc.lib.multipart.impl;

import javax.annotation.Nullable;

import alexiil.mc.lib.multipart.api.AbstractPart;

public class TransientPartIdentifier {
    public final AbstractPart part;
    public final @Nullable Object subPart;

    public TransientPartIdentifier(AbstractPart part) {
        this.part = part;
        this.subPart = null;
    }

    public TransientPartIdentifier(AbstractPart part, Object subPart) {
        this.part = part;
        this.subPart = subPart;
    }
}
