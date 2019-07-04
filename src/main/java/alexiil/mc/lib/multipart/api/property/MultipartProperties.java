/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.property;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.multipart.api.property.MultipartProperty.IntegerBoundProperty;
import alexiil.mc.lib.multipart.api.property.MultipartProperty.PreferedBooleanProperty;

public final class MultipartProperties {
    private MultipartProperties() {}

    /** The amount of light that a multiblock is emitting. Range 0-15 (inclusive) */
    public static final MultipartProperty<Integer> LIGHT_VALUE;

    /** The return value from {@link Block#emitsRedstonePower(BlockState)} */
    public static final MultipartProperty<Boolean> CAN_EMIT_REDSTONE;

    private static final Map<Direction, StrongRedstonePowerProperty> STRONG_REDSTONE_POWER;
    private static final Map<Direction, WeakRedstonePowerProperty> WEAK_REDSTONE_POWER;

    public static StrongRedstonePowerProperty getStrongRedstonePower(Direction side) {
        return STRONG_REDSTONE_POWER.get(side);
    }

    public static WeakRedstonePowerProperty getWeakRedstonePower(Direction side) {
        return WEAK_REDSTONE_POWER.get(side);
    }

    static {
        LIGHT_VALUE = new IntegerBoundProperty("LIGHT_VALUE", 0, 15, 0);
        CAN_EMIT_REDSTONE = new PreferedBooleanProperty("EMITS_REDSTONE", false);
        STRONG_REDSTONE_POWER = new EnumMap<>(Direction.class);
        WEAK_REDSTONE_POWER = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            STRONG_REDSTONE_POWER.put(dir, new StrongRedstonePowerProperty(dir));
            WEAK_REDSTONE_POWER.put(dir, new WeakRedstonePowerProperty(dir));
        }
    }

    public static abstract class RedstonePowerProperty extends IntegerBoundProperty {
        public final Direction side;

        public RedstonePowerProperty(String preText, Direction side) {
            super(preText + " Redstone Power " + side, 0, 15, 0);
            this.side = side;
        }
    }

    public static final class StrongRedstonePowerProperty extends RedstonePowerProperty {
        public StrongRedstonePowerProperty(Direction side) {
            super("Strong", side);
        }
    }

    public static final class WeakRedstonePowerProperty extends RedstonePowerProperty {
        public WeakRedstonePowerProperty(Direction side) {
            super("Weak", side);
        }
    }
}
