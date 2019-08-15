/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import java.util.List;

import alexiil.mc.lib.multipart.api.event.ContextlessEvent;
import alexiil.mc.lib.multipart.api.event.EventListener;
import alexiil.mc.lib.multipart.api.event.MultipartEvent;
import alexiil.mc.lib.multipart.api.event.MultipartEventExternalListener;

/** The event bus for {@link MultipartContainer}'s. */
public interface MultipartEventBus {

    /** @return The {@link MultipartContainer} for this {@link MultipartEventBus}. */
    MultipartContainer getContainer();

    /** Adds a listener for a specified {@link MultipartEvent} (and all of it's subclasses).
     * <p>
     * This makes only one guarantee for event listener ordering: all listeners added for the same key will be called in
     * the order that they are registered.
     * 
     * @param key The identifier for the listener, compared with identity equality (==) and <strong>not</strong> object
     *            equality. Multiple listeners can be added with the same key. If the caller is an {@link AbstractPart},
     *            and contained within this event bus's {@link #getContainer()} then it is strongly recommended that the
     *            {@link AbstractPart} is re-used, because when that part is removed from the {@link MultipartContainer}
     *            {@link #removeListeners(Object)} will be called with that part.
     * @param clazz The type of event to listen to.
     * @throws NullPointerException if any of the arguments are null. */
    <E extends MultipartEvent> ListenerInfo<E> addListener(Object key, Class<E> clazz, EventListener<E> listener);

    /** Adds a listener for a specified {@link MultipartEvent} that also implements {@link ContextlessEvent} with a
     * {@link Runnable}.
     * <p>
     * This makes only one guarantee for event listener ordering: all listeners added for the same key will be called in
     * the order that they are registered.
     * 
     * @param key The identifier for the listener, compared with identity equality (==) and <strong>not</strong> object
     *            equality. Multiple listeners can be added with the same key. If the caller is an {@link AbstractPart},
     *            and contained within this event bus's {@link #getContainer()} then it is strongly recommended that the
     *            {@link AbstractPart} is re-used, because when that part is removed from the {@link MultipartContainer}
     *            {@link #removeListeners(Object)} will be called with that part.
     * @param clazz The type of event to listen to.
     * @throws NullPointerException if any of the arguments are null. */
    default <E extends MultipartEvent & ContextlessEvent> ListenerInfo<E> addContextlessListener(Object key, Class<
        E> clazz, Runnable listener) {
        return addListener(key, clazz, new ContextlessListener<>(listener));
    }

    /** Adds a listener for a specified {@link MultipartEvent} (and all of it's subclasses), but also passes the
     * {@link MultipartContainer} that the event was fired from ito the event handler.
     * <p>
     * This makes only one guarantee for event listener ordering: all listeners added for the same key will be called in
     * the order that they are registered.
     * 
     * @param key The identifier for the listener, compared with identity equality (==) and <strong>not</strong> object
     *            equality. Multiple listeners can be added with the same key. If the caller is an {@link AbstractPart},
     *            and contained within this event bus's {@link #getContainer()} then it is strongly recommended that the
     *            {@link AbstractPart} is re-used, because when that part is removed from the {@link MultipartContainer}
     *            {@link #removeListeners(Object)} will be called with that part.
     * @param clazz The type of event to listen to.
     * @throws NullPointerException if any of the arguments are null. */
    default <E extends MultipartEvent> ListenerInfo<E> addExternalListener(Object key, Class<E> clazz,
        MultipartEventExternalListener<E> listener) {
        final MultipartContainer container = getContainer();
        return addListener(key, clazz, new ExternalListener<>(container, listener));
    }

    /** Fires the given event to all currently registered listeners.
     * 
     * @return True if any listeners received the given event, false if none did. This may be useful for optimisation
     *         purposes. */
    boolean fireEvent(MultipartEvent event);

    /** Removes all listeners that were added for the given key. */
    void removeListeners(Object key);

    /** @param key The identifier that was used in {@link #addListener(Object, Class, EventListener)}.
     * @return All listeners that were registered for the given key. */
    List<ListenerInfo<? extends MultipartEvent>> getListenersForKey(Object key);

    default boolean hasAnyListenersForKey(Object key) {
        return !getListenersForKey(key).isEmpty();
    }

    /** @return A list of every {@link ListenerInfo} that will receive events of the given type. */
    <E extends MultipartEvent> List<? extends ListenerInfo<? extends E>> getListeners(Class<E> clazz);

    /** @return A list of every {@link ListenerInfo} current registered to this event bus. */
    List<? extends ListenerInfo<?>> getAllListeners();

    default boolean hasAnyListenersFor(Class<? extends MultipartEvent> clazz) {
        return !getListeners(clazz).isEmpty();
    }

    default boolean hasAnyListeners() {
        return !getAllListeners().isEmpty();
    }

    /** Information on a single registered listener. */
    public interface ListenerInfo<E extends MultipartEvent> {
        Object getKey();

        Class<E> getListenerClass();

        EventListener<E> getListener();

        /** Removes this listener. */
        void remove();
    }

    /** An {@link EventListener} for a {@link ContextlessEvent} that is invoked with a {@link Runnable}. */
    public static final class ContextlessListener<E extends MultipartEvent & ContextlessEvent>
        implements EventListener<E> {

        public final Runnable listener;

        public ContextlessListener(Runnable listener) {
            this.listener = listener;
        }

        @Override
        public void onEvent(E event) {
            listener.run();
        }

        @Override
        public String toString() {
            return "{Runnable: " + listener + "}";
        }
    }

    public static final class ExternalListener<E extends MultipartEvent> implements EventListener<E> {
        public final MultipartContainer container;
        public final MultipartEventExternalListener<E> listener;

        public ExternalListener(MultipartContainer container, MultipartEventExternalListener<E> listener) {
            this.container = container;
            this.listener = listener;
        }

        @Override
        public void onEvent(E event) {
            listener.onEvent(container, event);
        }

        @Override
        public String toString() {
            return "{External: " + listener + "}";
        }
    }
}
