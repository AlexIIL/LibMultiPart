package alexiil.mc.lib.multipart.impl;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultiPartContainer.IPartOffer;
import alexiil.mc.lib.multipart.api.MultiPartHolder;
import alexiil.mc.lib.multipart.api.MultiPartUtil;

/** Contains the backend for {@link MultiPartUtil}. */
public final class MultiPartUtilImpl {

    public static PartContainer get(World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultiPartBlockEntity) {
            return ((MultiPartBlockEntity) be).container;
        }
        return null;
    }

    @Nullable
    public static IPartOffer offerNewPart(World world, BlockPos pos, Function<MultiPartHolder, AbstractPart> creator) {

        // See if there's an existing multipart that we can add to
        PartContainer existing = get(world, pos);
        if (existing != null) {
            return existing.offerNewPart(creator);
        }

        // There's not: so we need to place a new one
        if (!world.isAir(pos) || world.getFluidState(pos).getFluid() != Fluids.EMPTY) {
            return null;
        }

        MultiPartBlockEntity be = new MultiPartBlockEntity();
        be.setWorld(world);
        be.setPos(pos);
        PartContainer container = new PartContainer(be);
        PartHolder holder = new PartHolder(container, creator);
        VoxelShape collisionShape = holder.part.getCollisionShape();

        if (!world.intersectsEntities(null, collisionShape)) {
            return null;
        }
        return new IPartOffer() {
            @Override
            public MultiPartHolder getHolder() {
                return holder;
            }

            @Override
            public void apply() {
                world.setBlockState(pos, LibMultiPart.BLOCK.getDefaultState());
                MultiPartBlockEntity newBe = (MultiPartBlockEntity) world.getBlockEntity(pos);
                assert newBe != null;
                newBe.container = container;
                container.blockEntity = newBe;
                container.addPartInternal(holder);
            }
        };
    }
}
