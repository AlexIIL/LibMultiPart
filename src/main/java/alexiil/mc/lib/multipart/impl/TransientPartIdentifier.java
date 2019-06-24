/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.Objects;

import javax.annotation.Nullable;

import alexiil.mc.lib.multipart.api.AbstractPart;

public class TransientPartIdentifier {
    public final AbstractPart part;
    public final @Nullable Object subPart;

    public TransientPartIdentifier(AbstractPart part) {
        this.part = part;
        this.subPart = null;
    }

    public TransientPartIdentifier(AbstractPart part, Object subPart) {
        this.part = part;
        this.subPart = subPart;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() != getClass()) {
            return false;
        }
        TransientPartIdentifier other = (TransientPartIdentifier) obj;
        return part == other.part && Objects.equals(subPart, other.subPart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(part, subPart);
    }

    @Override
    public String toString() {
        return "TransientPartIdentifier{ part = " + part + ", subPart = " + subPart + " }";
    }
}
