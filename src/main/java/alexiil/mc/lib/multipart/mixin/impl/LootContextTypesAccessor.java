package alexiil.mc.lib.multipart.mixin.impl;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;

@Mixin(LootContextTypes.class)
public interface LootContextTypesAccessor {

    @Invoker
    public static LootContextType register(String name, Consumer<LootContextType.Builder> type) {
        throw new NullPointerException("LMP");
    }
}
