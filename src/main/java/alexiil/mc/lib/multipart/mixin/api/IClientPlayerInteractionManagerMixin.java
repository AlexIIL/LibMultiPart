package alexiil.mc.lib.multipart.mixin.api;

import net.minecraft.util.math.BlockPos;

public interface IClientPlayerInteractionManagerMixin {

    BlockPos libmultipart_getCurrentBreakPosition();

    Object libmultipart_getPartKey();
}
