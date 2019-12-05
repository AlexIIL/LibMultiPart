/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

import javax.annotation.Nullable;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.SubdividedPart;

public interface PartBreakContext {

    /** @return The part that is being broken. This might not be the same as the part that is being rendered! */
    AbstractPart getPart();

    /** @return The key for the {@link SubdividedPart} instance. */
    @Nullable
    Object getSubPart();

    @Nullable
    default <Sub> Sub getSubPart(SubdividedPart<Sub> part) {
        Object sub = getSubPart();
        if (sub != null && part != null) {
            if (part.getSubpartKeyClass().isInstance(sub)) {
                return part.getSubpartKeyClass().cast(sub);
            }
        }
        return null;
    }
}
