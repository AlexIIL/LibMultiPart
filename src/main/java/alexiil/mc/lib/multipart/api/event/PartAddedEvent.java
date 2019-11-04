/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartContainer;

/** Fired whenever an {@link AbstractPart} is added to a {@link MultipartContainer}. Note that that part itself will
 * *also* receive this event. */
public final class PartAddedEvent extends MultipartEvent {
    public final AbstractPart part;

    public PartAddedEvent(AbstractPart part) {
        this.part = part;
    }
}
