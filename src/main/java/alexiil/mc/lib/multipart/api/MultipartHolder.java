/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

/** Wrapper interface for an {@link AbstractPart} in a {@link MultipartContainer}. */
public interface MultipartHolder {

    MultipartContainer getContainer();

    AbstractPart getPart();

    /** Removes this {@link #getPart()} from the container. */
    void remove();
}
