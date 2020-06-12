/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import net.minecraft.loot.context.LootContext;
import net.minecraft.util.Util;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.PartLootParams;
import alexiil.mc.lib.multipart.api.PartLootParams.BrokenPart;
import alexiil.mc.lib.multipart.api.PartLootParams.BrokenSinglePart;
import alexiil.mc.lib.multipart.api.PartLootParams.BrokenSubPart;
import alexiil.mc.lib.multipart.api.SubdividedPart;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

public final class TransientPartIdentifier {
    public final AbstractPart part;
    public final ExtraIdData extra;

    public TransientPartIdentifier(AbstractPart part) {
        this.part = part;
        PartHolder holder = (PartHolder) part.holder;
        Set<PartHolder> parts = PartContainer.getAllRemoved(holder);
        if (parts.size() <= 1) {
            extra = new IdAdditional(Collections.emptySet());
        } else {
            Set<AbstractPart> additional = new ObjectOpenCustomHashSet<>(Util.identityHashStrategy());
            for (PartHolder h : parts) {
                if (h.part != part) {
                    additional.add(h.part);
                }
            }
            extra = new IdAdditional(additional);
        }
    }

    public <Sub> TransientPartIdentifier(SubdividedPart<Sub> part, Sub subpart) {
        this.part = (AbstractPart) part;
        this.extra = new IdSubPart<>(part, subpart);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() != getClass()) {
            return false;
        }
        TransientPartIdentifier other = (TransientPartIdentifier) obj;
        return part == other.part && Objects.equals(extra, other.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(part, extra);
    }

    @Override
    public String toString() {
        return "TransientPartIdentifier{ part = " + part + ", extra = " + extra + " }";
    }

    public void putLootContext(LootContext.Builder builder) {
        builder.parameter(PartLootParams.BROKEN_PART, new BrokenSinglePart(part));
        extra.putLootContext(builder);
    }

    public static abstract class ExtraIdData {
        ExtraIdData() {}

        protected abstract void putLootContext(LootContext.Builder builder);
    }

    public static final class IdSubPart<Sub> extends ExtraIdData {
        public final SubdividedPart<Sub> part;
        public final Sub subpart;

        public IdSubPart(SubdividedPart<Sub> part, Sub subpart) {
            this.part = part;
            this.subpart = subpart;
        }

        @Override
        protected void putLootContext(LootContext.Builder builder) {
            builder.parameter(PartLootParams.BROKEN_PART, new BrokenSubPart<>(part, subpart));
            builder.parameter(PartLootParams.ADDITIONAL_PARTS, new BrokenPart[0]);
        }
    }

    public static final class IdAdditional extends ExtraIdData {
        public final Set<AbstractPart> additional;

        public IdAdditional(Set<AbstractPart> additional) {
            this.additional = additional;
        }

        @Override
        protected void putLootContext(LootContext.Builder builder) {
            BrokenPart[] array = new BrokenPart[additional.size()];
            int i = 0;
            for (AbstractPart part : additional) {
                array[i++] = new BrokenSinglePart(part);
            }
            builder.parameter(PartLootParams.ADDITIONAL_PARTS, array);
        }
    }
}
