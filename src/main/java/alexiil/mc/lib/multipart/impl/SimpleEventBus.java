/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.MultipartEventBus;
import alexiil.mc.lib.multipart.api.event.EventListener;
import alexiil.mc.lib.multipart.api.event.MultipartEvent;
import alexiil.mc.lib.multipart.api.event.PartListenerAdded;
import alexiil.mc.lib.multipart.api.event.PartListenerRemoved;

/** A simple, dumb, {@link ArrayList} based approach for storing event listeners. */
public class SimpleEventBus implements MultipartEventBus {

    private static final SingleListener<?>[] EMPTY_LISTENER_ARRAY = new SingleListener[0];

    private final PartContainer container;
    private final List<SingleListener<?>> listeners = new ArrayList<>();

    /** Used for iterating over the listeners in {@link #fireEvent(MultipartEvent)}. */
    private SingleListener<?>[] packedListeners = EMPTY_LISTENER_ARRAY;

    private int eventCallLevel = 0;
    private boolean didListenersChange = false;

    private final List<SingleListener<?>> listenersChanged = new ArrayList<>();
    private final List<Boolean> listenerChangedToAdd = new ArrayList<>();

    public SimpleEventBus(PartContainer container) {
        this.container = container;
    }

    @Override
    public MultipartContainer getContainer() {
        return container;
    }

    @Override
    public boolean fireEvent(MultipartEvent event) {
        boolean anyHandled = false;
        assert eventCallLevel >= 0;
        try {
            eventCallLevel++;
            for (SingleListener<?> listener : packedListeners) {
                anyHandled |= listener.onEvent(event);
            }
        } finally {
            eventCallLevel--;
        }
        assert eventCallLevel >= 0;
        if (eventCallLevel == 0 && didListenersChange) {
            packedListeners = listeners.toArray(new SingleListener[0]);
            didListenersChange = false;
            for (int i = 0; i < listenersChanged.size(); i++) {
                SingleListener<?> single = listenersChanged.get(i);
                Boolean didAdd = listenerChangedToAdd.get(i);
                if (didAdd) {
                    fireListenerAddEvent(single);
                } else {
                    fireListenerRemoveEvent(single);
                }
            }
            listenerChangedToAdd.clear();
            listenersChanged.clear();
        }
        return anyHandled;
    }

    @Override
    public <E extends MultipartEvent> void addListener(Object key, Class<E> clazz, EventListener<E> listener) {
        SingleListener<E> single = new SingleListener<>(key, clazz, listener);
        listeners.add(single);
        if (eventCallLevel > 0) {
            listenersChanged.add(single);
            listenerChangedToAdd.add(true);
            didListenersChange = true;
        } else {
            fireListenerAddEvent(single);
        }
        packedListeners = listeners.toArray(new SingleListener[0]);
    }

    protected <E extends MultipartEvent> void fireListenerAddEvent(SingleListener<E> single) {
        container.onListenerAdded(single);
        fireEvent(new PartListenerAdded<>(single.key, single.clazz, single.listener));
    }

    protected <E extends MultipartEvent> void fireListenerRemoveEvent(SingleListener<E> single) {
        container.onListenerRemoved(single);
        fireEvent(new PartListenerRemoved<>(single.key, single.clazz, single.listener));
    }

    void clearListeners() {
        ListIterator<SingleListener<?>> iter = listeners.listIterator(listeners.size());
        boolean removedAny = false;
        while (iter.hasPrevious()) {
            SingleListener<?> single = iter.previous();
            iter.remove();
            removedAny = true;
            if (eventCallLevel > 0) {
                listenersChanged.add(single);
                listenerChangedToAdd.add(false);
                didListenersChange = true;
            } else {
                fireListenerRemoveEvent(single);
            }
        }
        if (removedAny) {
            packedListeners = listeners.toArray(new SingleListener[0]);
        }
    }

    @Override
    public void removeListeners(Object key) {
        ListIterator<SingleListener<?>> iter = listeners.listIterator(listeners.size());
        boolean removedAny = false;
        while (iter.hasPrevious()) {
            SingleListener<?> single = iter.previous();
            if (single.key == key) {
                iter.remove();
                removedAny = true;
                if (eventCallLevel > 0) {
                    listenersChanged.add(single);
                    listenerChangedToAdd.add(false);
                    didListenersChange = true;
                } else {
                    fireListenerRemoveEvent(single);
                }
            }
        }
        if (removedAny) {
            packedListeners = listeners.toArray(new SingleListener[0]);
        }
    }

    @Override
    public boolean hasAnyListeners() {
        return !listeners.isEmpty();
    }

    @Override
    public List<ListenerInfo<?>> getListenersForKey(Object key) {
        List<ListenerInfo<?>> list = new ArrayList<>();
        for (SingleListener<?> single : listeners) {
            if (single.key == key) {
                list.add(single);
            }
        }
        return list;
    }

    @Override
    public boolean hasAnyListenersForKey(Object key) {
        for (SingleListener<?> single : listeners) {
            if (single.key == key) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <E extends MultipartEvent> List<ListenerInfo<? extends E>> getListeners(Class<E> clazz) {
        List<ListenerInfo<? extends E>> list = new ArrayList<>();
        for (SingleListener<?> single : listeners) {
            if (single.clazz.isAssignableFrom(clazz)) {
                list.add((ListenerInfo<? extends E>) single);
            }
        }
        return list;
    }

    @Override
    public List<? extends ListenerInfo<?>> getAllListeners() {
        return Collections.unmodifiableList(listeners);
    }

    static class SingleListener<E extends MultipartEvent> implements ListenerInfo<E> {

        final Object key;
        final Class<E> clazz;
        final EventListener<E> listener;

        public SingleListener(Object key, Class<E> clazz, EventListener<E> listener) {
            this.key = key;
            this.clazz = clazz;
            this.listener = listener;
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public Class<E> getListenerClass() {
            return clazz;
        }

        @Override
        public EventListener<E> getListener() {
            return listener;
        }

        boolean onEvent(MultipartEvent event) {
            if (clazz.isInstance(event)) {
                listener.onEvent(clazz.cast(event));
                return true;
            }
            return false;
        }
    }
}
