/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import alexiil.mc.lib.multipart.api.MultipartEventBus.ListenerInfo;
import alexiil.mc.lib.multipart.api.event.PartTickEvent;

/** Wrapper interface for an {@link AbstractPart} in a {@link MultipartContainer}. */
public interface MultipartHolder {

    public static final long NOT_ADDED_UNIQUE_ID = Long.MIN_VALUE;

    MultipartContainer getContainer();

    AbstractPart getPart();

    /** Removes this {@link #getPart()} from the container. */
    void remove();

    /** @return The (container-only) unique ID for this part holder, or {@link #NOT_ADDED_UNIQUE_ID} if this hasn't been
     *         added to it's container. */
    long getUniqueId();

    /** @return True if this holder is contained in it's {@link #getContainer()}, false otherwise. */
    boolean isPresent();

    /** Makes this {@link AbstractPart} depend on another part. */
    void addRequiredPart(AbstractPart other);

    /** Removes the requirement this has for the given part. (This inverts {@link #addRequiredPart(AbstractPart)} */
    void removeRequiredPart(AbstractPart other);

    /** If {@link MultipartContainer#hasTicked()} returns true then this will just call the runnable directly, and
     * return. Otherwise this will add an event listener for the {@link PartTickEvent}, and remove it when it is first
     * ran. Enqueue's a {@link Runnable} to be run on the first {@link PartTickEvent} */
    default void enqueueFirstTickTask(Runnable runnable) {
        if (getContainer().hasTicked()) {
            runnable.run();
        } else {
            MultipartEventBus eventBus = getContainer().getEventBus();
            ListenerInfo<?>[] infos = new ListenerInfo<?>[1];
            infos[0] = eventBus.addContextlessListener(getPart(), PartTickEvent.class, () -> {
                if (infos[0] != null) {
                    runnable.run();
                    infos[0].remove();
                    infos[0] = null;
                }
            });
        }
    }
}
