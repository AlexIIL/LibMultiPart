/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import alexiil.mc.lib.multipart.api.AbstractPart.ItemDropTarget;

final class ItemDropCollector implements ItemDropTarget {
    private final DefaultedList<ItemStack> drops;

    ItemDropCollector(DefaultedList<ItemStack> drops) {
        this.drops = drops;
    }

    @Override
    public void drop(ItemStack stack) {
        drops.add(stack);
    }

    @Override
    public void drop(ItemStack stack, Vec3d pos) {
        drops.add(stack);
    }

    @Override
    public void drop(ItemStack stack, Vec3d pos, Vec3d velocity) {
        drops.add(stack);
    }

    @Override
    public boolean dropsAsEntity() {
        return false;
    }
}
