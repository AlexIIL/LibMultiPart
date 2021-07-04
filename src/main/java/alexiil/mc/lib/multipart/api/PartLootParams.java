/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextType;

import alexiil.mc.lib.multipart.impl.LibMultiPart;
import alexiil.mc.lib.multipart.mixin.impl.LootContextTypesAccessor;

/** Stores some {@link LootContextParameter}s for LMP. */
public final class PartLootParams {
    private PartLootParams() {}

    /** The primary {@link AbstractPart} that was broken */
    public static final LootContextParameter<BrokenPart> BROKEN_PART;

    /** Any additional {@link AbstractPart}'s that were broken because they
     * {@link AbstractPart#addRequiredPart(AbstractPart) required} the main broken part. */
    public static final LootContextParameter<BrokenPart[]> ADDITIONAL_PARTS;

    public static final LootContextType PART_TYPE;

    static {
        BROKEN_PART = new LootContextParameter<>(LibMultiPart.id("broken_part"));
        ADDITIONAL_PARTS = new LootContextParameter<>(LibMultiPart.id("additional_parts"));
        PART_TYPE = LootContextTypesAccessor.callRegister(
            "libmultipart:part", builder -> builder//
                // Block
                .require(LootContextParameters.BLOCK_STATE)//
                .require(LootContextParameters.ORIGIN)//
                .require(LootContextParameters.TOOL)//
                .allow(LootContextParameters.THIS_ENTITY)//
                .allow(LootContextParameters.BLOCK_ENTITY)//
                .allow(LootContextParameters.EXPLOSION_RADIUS)//
                // Ours
                .require(BROKEN_PART)//
                .require(ADDITIONAL_PARTS)//
        );
    }

    /** An {@link AbstractPart} that was broken.
     * <p>
     * There are two subclasses: {@link BrokenSinglePart} (for normal parts), and {@link BrokenSubPart} (for
     * {@link SubdividedPart}s). */
    public static abstract class BrokenPart {
        private BrokenPart() {
            // Private: only BrokenSinglePart and BrokenSubPart are meant to extend this
        }

        /** @return The {@link AbstractPart} associated with this. */
        public abstract AbstractPart getPart();
    }

    public static final class BrokenSinglePart extends BrokenPart {
        public final AbstractPart part;

        public BrokenSinglePart(AbstractPart part) {
            this.part = part;
        }

        @Override
        public AbstractPart getPart() {
            return part;
        }
    }

    public static final class BrokenSubPart<Sub> extends BrokenPart {
        public final SubdividedPart<Sub> mainPart;
        public final Sub subPart;

        public BrokenSubPart(SubdividedPart<Sub> mainPart, Sub subPart) {
            if (!(mainPart instanceof AbstractPart)) {
                throw new ClassCastException(
                    mainPart.getClass() + " implements " + SubdividedPart.class + " but doesn't extend "
                        + AbstractPart.class
                );
            }
            this.mainPart = mainPart;
            this.subPart = subPart;
        }

        @Override
        public AbstractPart getPart() {
            return (AbstractPart) mainPart;
        }
    }
}
