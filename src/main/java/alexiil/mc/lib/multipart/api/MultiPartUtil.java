package alexiil.mc.lib.multipart.api;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.api.MultiPartContainer.IPartOffer;
import alexiil.mc.lib.multipart.impl.MultiPartUtilImpl;

/** Contains various utilities for creating, accessing, or interacting with {@link MultiPartContainer}'s in a
 * {@link World}. */
public final class MultiPartUtil {
    private MultiPartUtil() {}

    @Nullable
    public static MultiPartContainer get(World world, BlockPos pos) {
        return MultiPartUtilImpl.get(world, pos);
    }

    public static IPartOffer offerNewPart(World world, BlockPos pos, Function<MultiPartHolder, AbstractPart> creator) {
        return MultiPartUtilImpl.offerNewPart(world, pos, creator);
    }
}
