/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.property;

import alexiil.mc.lib.multipart.api.event.MultipartEvent;

public class PartPropertyChangedEvent<T> extends MultipartEvent {
    public final MultipartProperty<T> property;
    public final T oldValue, newValue;

    public PartPropertyChangedEvent(MultipartProperty<T> property, T oldValue, T newValue) {
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
