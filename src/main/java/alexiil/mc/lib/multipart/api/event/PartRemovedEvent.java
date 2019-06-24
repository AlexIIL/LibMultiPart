/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.AbstractPart;

public final class PartRemovedEvent extends MultiPartEvent {
    public final AbstractPart removed;

    public PartRemovedEvent(AbstractPart removed) {
        this.removed = removed;
    }
}
