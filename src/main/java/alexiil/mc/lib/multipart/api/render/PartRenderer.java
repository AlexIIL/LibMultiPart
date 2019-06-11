package alexiil.mc.lib.multipart.api.render;

import alexiil.mc.lib.multipart.api.AbstractPart;

public interface PartRenderer<P extends AbstractPart> {
    void render(P part, float partialTicks, int breakProgress);
}
