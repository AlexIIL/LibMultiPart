package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.MultiPartContainer;

@FunctionalInterface
public interface MultiPartEventExternalListener<E extends MultiPartEvent> {
    void onEvent(MultiPartContainer container, E event);
}
