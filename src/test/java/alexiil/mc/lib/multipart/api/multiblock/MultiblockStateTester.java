/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.multiblock;

import org.junit.Assert;
import org.junit.Test;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import alexiil.mc.lib.multipart.api.multiblock.MultiblockState.MultiblockStateBuilder;
import alexiil.mc.lib.multipart.api.multiblock.blockstate.MultiblockPointerState;
import alexiil.mc.lib.multipart.api.multiblock.blockstate.MultiblockSingleState.MultiblockStateGetter;

public class MultiblockStateTester {
    @Test
    public void testSimple() {
        MultiblockStateBuilder builder = new MultiblockStateBuilder(BlockPos.ORIGIN);
        builder.addSlave(new BlockPos(0, 0, 1));
        MultiblockState stateA = builder.build();

        builder.printInfo(System.out::println);
        stateA.printInfo(System.out::println);

        builder.addSlave(new BlockPos(1, 0, 0));
        MultiblockState stateB = builder.build();

        builder.printInfo(System.out::println);
        stateB.printInfo(System.out::println);
    }

    @Test
    public void testLine() {
        MultiblockStateBuilder builder = new MultiblockStateBuilder(BlockPos.ORIGIN);
        for (int i = 1; i <= 20; i++) {
            builder.addSlave(new BlockPos(i, 0, 0), MultiblockPointerState.get(Direction.WEST));
        }

        builder.printInfo(System.out::println);
        MultiblockState state = builder.build();
        state.printInfo(System.out::println);

        MultiblockStateGetter getter = pos -> {
            if (state.master.equals(pos)) {
                return MultiblockPointerState.TempMaster.INSTANCE;
            }
            return state.getPointerState(pos);
        };
        for (int i = 0; i <= 20; i++) {
            BlockPos found = getter.findMaster(new BlockPos(i, 0, 0));
            Assert.assertEquals(BlockPos.ORIGIN, found);
        }
    }

    @Test
    public void testDeadEndLine() {
        for (int swap = 0; swap < 2; swap++) {
            MultiblockStateBuilder builder = new MultiblockStateBuilder(BlockPos.ORIGIN);

            for (int i = 1; i <= (20 - swap); i++) {
                if (swap == 1 && i == 1) {
                    builder.addSlave(new BlockPos(i, 0, 0), MultiblockPointerState.get(Direction.WEST));
                    continue;
                }
                Direction dir = i % 2 == swap ? Direction.WEST : Direction.EAST;
                builder.addSlave(new BlockPos(i, 0, 0), MultiblockPointerState.get(dir, dir));
            }

            builder.printInfo(System.out::println);
            MultiblockState state = builder.build();
            state.printInfo(System.out::println);

            MultiblockStateGetter getter = pos -> {
                if (state.master.equals(pos)) {
                    return MultiblockPointerState.TempMaster.INSTANCE;
                }
                return state.getPointerState(pos);
            };
            for (int i = 0; i <= (20 - swap); i++) {
                System.out.println("Searching from " + i);
                BlockPos found = getter.findMaster(new BlockPos(i, 0, 0));
                Assert.assertEquals("i = " + i, BlockPos.ORIGIN, found);
            }
        }
    }
}
