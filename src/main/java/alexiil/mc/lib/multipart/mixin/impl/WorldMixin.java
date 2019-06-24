package alexiil.mc.lib.multipart.mixin.impl;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.mixin.api.IUnloadableBlockEntity;

@Mixin(World.class)
public class WorldMixin {

    @Shadow
    private List<BlockEntity> unloadedBlockEntities;

    @Inject(at = @At("HEAD"), method = { "tickBlockEntities()V" })
    public void tick(CallbackInfo ci) {
        for (BlockEntity entity : unloadedBlockEntities) {
            if (entity instanceof IUnloadableBlockEntity) {
                ((IUnloadableBlockEntity) entity).onChunkUnload();
            }
        }
    }
}
