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

    /** @return The current index of this holder in {@link #getContainer()}.{@link MultipartContainer#getAllParts()
     *         getAllParts()} */
    default int getPartIndex() {
        return getContainer().getAllParts().indexOf(getPart());
    }

    /** @return True if this holder is contained in it's {@link #getContainer()}, false otherwise. */
    default boolean isPresent() {
        return getPartIndex() >= 0;
    }

    /** Makes this {@link AbstractPart} depend on another part. */
    void addRequiredPart(AbstractPart other);

    /** Removes the requirement this has for the given part. (This inverts {@link #addRequiredPart(AbstractPart)} */
    void removeRequiredPart(AbstractPart other);
}
