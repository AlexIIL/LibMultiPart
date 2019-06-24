/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.MultiPartEventBus;

/** Fired whenever {@link MultiPartEventBus#removeListeners(Object)} is called. This is fired once for every listener
 * that was removed, <em>after</em> it is actually removed. */
public final class PartListenerRemoved<E extends MultiPartEvent> extends MultiPartEvent {

    public final Object key;
    public final Class<E> eventClass;
    public final EventListener<E> eventListener;

    public PartListenerRemoved(Object key, Class<E> eventClass, EventListener<E> eventListener) {
        this.key = key;
        this.eventClass = eventClass;
        this.eventListener = eventListener;
    }
}
