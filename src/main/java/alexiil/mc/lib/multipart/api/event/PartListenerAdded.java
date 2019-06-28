/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.MultipartEventBus;

/** Fired whenever {@link MultipartEventBus#addListener(Object, Class, EventListener)} is called. This is fired
 * <em>before</em> it is actually added.
 * <p>
 * This is provided only for */
public final class PartListenerAdded<E extends MultipartEvent> extends MultipartEvent {

    public final Object key;
    public final Class<E> eventClass;
    public final EventListener<E> eventListener;

    public PartListenerAdded(Object key, Class<E> eventClass, EventListener<E> eventListener) {
        this.key = key;
        this.eventClass = eventClass;
        this.eventListener = eventListener;
    }
}
