/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.property;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartContainer;

public interface MultipartPropertyContainer {

    MultipartContainer getContainer();

    /** @return The current combined value for the given property across all keys, or the
     *         {@link MultipartProperty#defaultValue default value} if no values have been explicitly set. */
    <T> T getValue(MultipartProperty<T> property);

    /** @param key The identifier for the source object that is setting the property, compared with identity equality
     *            (==) and <strong>not</strong> object equality. Multiple different sources can all set a different
     *            value for the given property. If the caller is an {@link AbstractPart}, and contained within
     *            {@link #getContainer()} then it is strongly recommended that the {@link AbstractPart} is re-used,
     *            because when that part is removed from the {@link MultipartContainer} {@link #clearValues(Object)}
     *            will be called with that part. */
    <T> void setValue(Object key, MultipartProperty<T> property, T value);

    <T> void clearValue(Object key, MultipartProperty<T> property);

    /** Removes all property values set for the given key. */
    void clearValues(Object key);
}
