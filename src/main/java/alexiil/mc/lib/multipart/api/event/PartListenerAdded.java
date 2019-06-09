package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.MultiPartEventBus;

/** Fired whenever {@link MultiPartEventBus#addListener(Object, Class, EventListener)} is called. This is fired
 * <em>before</em> it is actually added.
 * <p>
 * This is provided only for */
public final class PartListenerAdded<E extends MultiPartEvent> extends MultiPartEvent {

    public final Object key;
    public final Class<E> eventClass;
    public final EventListener<E> eventListener;

    public PartListenerAdded(Object key, Class<E> eventClass, EventListener<E> eventListener) {
        this.key = key;
        this.eventClass = eventClass;
        this.eventListener = eventListener;
    }
}
