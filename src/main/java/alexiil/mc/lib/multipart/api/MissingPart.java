/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import alexiil.mc.lib.multipart.impl.MissingPartImpl;

/** A special kind of {@link AbstractPart} which is used whenever a part cannot be read from NBT. */
public abstract class MissingPart extends AbstractPart {
    protected MissingPart(MissingPartDefinition definition, MultipartHolder holder) {
        super(definition, holder);
        if (getClass() != MissingPartImpl.class) {
            throw new IllegalStateException("Only LMP's implementation of MissingPart is permitted!");
        }
    }
}
