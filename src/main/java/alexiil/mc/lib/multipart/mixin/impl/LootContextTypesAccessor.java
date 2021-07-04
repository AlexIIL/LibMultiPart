/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;

@Mixin(LootContextTypes.class)
public interface LootContextTypesAccessor {

    @Invoker
    public static LootContextType libmultipart_register(String name, Consumer<LootContextType.Builder> type) {
        throw new NullPointerException("LMP");
    }
}
