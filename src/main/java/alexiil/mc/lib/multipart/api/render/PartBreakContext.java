package alexiil.mc.lib.multipart.api.render;

import javax.annotation.Nullable;

import alexiil.mc.lib.multipart.api.AbstractPart;

public interface PartBreakContext {
    AbstractPart getPart();

    @Nullable
    Object getSubPart();
}
