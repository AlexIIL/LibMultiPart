/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

/** Used by the static part baker to generate the quad list. */
public abstract class PartModelKey {

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

}
