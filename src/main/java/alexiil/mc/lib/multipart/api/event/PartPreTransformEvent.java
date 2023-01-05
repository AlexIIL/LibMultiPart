/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

/** Fired before any parts are transformed.
 *
 * @see PartTransformEvent */
public final class PartPreTransformEvent extends MultipartEvent implements ContextlessEvent {
    public static final PartPreTransformEvent INSTANCE = new PartPreTransformEvent();

    private PartPreTransformEvent() {
    }
}