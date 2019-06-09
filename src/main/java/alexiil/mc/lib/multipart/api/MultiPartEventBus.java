package alexiil.mc.lib.multipart.api;

import java.util.List;

import alexiil.mc.lib.multipart.api.event.EventListener;
import alexiil.mc.lib.multipart.api.event.MultiPartEvent;
import alexiil.mc.lib.multipart.api.event.MultiPartEventExternalListener;

/** The event bus for {@link MultiPartContainer}'s. */
public interface MultiPartEventBus {

    /** @return The {@link MultiPartContainer} for this {@link MultiPartEventBus}. */
    MultiPartContainer getContainer();

    /** Adds a listener for a specified {@link MultiPartEvent} (and all of it's subclasses).
     * <p>
     * This makes only one guarantee for event listener ordering: all listeners added for the same key will be called in
     * the order that they are registered.
     * 
     * @param key The identifier for the listener, compared with identity equality (==) and <strong>not</strong> object
     *            equality. Multiple listeners can be added with the same key. If the caller is an {@link AbstractPart},
     *            and contained within this event bus's {@link #getContainer()} then it is strongly recommended that the
     *            {@link AbstractPart} is re-used, because when that part is removed from the {@link MultiPartContainer}
     *            {@link #removeListeners(Object)} will be called with that part.
     * @param clazz The type of event to listen to.
     * @throws NullPointerException if any of the arguments are null. */
    <E extends MultiPartEvent> void addListener(Object key, Class<E> clazz, EventListener<E> listener);

    /** @see #addListener(Object, Class, EventListener) */
    default <E extends MultiPartEvent> void addExternalListener(Object key, Class<E> clazz,
        MultiPartEventExternalListener<E> listener) {
        final MultiPartContainer container = getContainer();
        addListener(key, clazz, new ExternalListener<>(container, listener));
    }

    /** Fires the given event to all currently registered listeners.
     * 
     * @return True if any listeners received the given event, false if none did. This may be useful for optimisation
     *         purposes. */
    boolean fireEvent(MultiPartEvent event);

    /** Removes all listeners that were added for the given key. */
    void removeListeners(Object key);

    /** @param key The identifier that was used in {@link #addListener(Object, Class, EventListener)}.
     * @return All listeners that were registered for the given key. */
    List<ListenerInfo<? extends MultiPartEvent>> getListenersForKey(Object key);

    default boolean hasAnyListenersForKey(Object key) {
        return !getListenersForKey(key).isEmpty();
    }

    /** @return A list of every {@link ListenerInfo} that will receive events of the given type. */
    <E extends MultiPartEvent> List<? extends ListenerInfo<? extends E>> getListeners(Class<E> clazz);

    /** @return A list of every {@link ListenerInfo} current registered to this event bus. */
    List<? extends ListenerInfo<?>> getAllListeners();

    default boolean hasAnyListenersFor(Class<? extends MultiPartEvent> clazz) {
        return !getListeners(clazz).isEmpty();
    }

    default boolean hasAnyListeners() {
        return !getAllListeners().isEmpty();
    }

    /** Information on a single registered listener. */
    public interface ListenerInfo<E extends MultiPartEvent> {
        Object getKey();

        Class<E> getListenerClass();

        EventListener<E> getListener();
    }

    public static final class ExternalListener<E extends MultiPartEvent> implements EventListener<E> {
        public final MultiPartContainer container;
        public final MultiPartEventExternalListener<E> listener;

        public ExternalListener(MultiPartContainer container, MultiPartEventExternalListener<E> listener) {
            this.container = container;
            this.listener = listener;
        }

        @Override
        public void onEvent(E event) {
            listener.onEvent(container, event);
        }
    }
}
