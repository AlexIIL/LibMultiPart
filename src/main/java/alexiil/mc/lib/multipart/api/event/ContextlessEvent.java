/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.MultipartEventBus;

/** Marker interface for {@link MultipartEvent}'s that don't come with any context: for example {@link PartTickEvent} is
 * a singleton, so you never care about anything about the event. (As such it is highly recommended that every class
 * that implements this is also final, as listeners won't know which subclass of this it is). MultipartEvent's that
 * implement this interface can be listened to via a {@link Runnable} instead of an {@link EventListener} in
 * {@link MultipartEventBus#addContextlessListener(Object, Class, Runnable)} */
public interface ContextlessEvent {
    // No methods need to be defined.
}
