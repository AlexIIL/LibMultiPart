package alexiil.mc.lib.multipart.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeProvider;
import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;

public class MultiPartBlock extends Block implements BlockEntityProvider, IBlockMultipart<TransientPartIdentifier>,
    AttributeProvider {

    public static final VoxelShape MISSING_PARTS_SHAPE = VoxelShapes.union(
        // X
        createCuboidShape(0, 0, 0, 16, 4, 4), createCuboidShape(0, 12, 0, 16, 16, 4), //
        createCuboidShape(0, 0, 12, 16, 4, 16), createCuboidShape(0, 12, 12, 16, 16, 16), //
        // Y
        createCuboidShape(0, 0, 0, 4, 16, 4), createCuboidShape(12, 0, 0, 16, 16, 4), //
        createCuboidShape(0, 0, 12, 4, 16, 16), createCuboidShape(12, 0, 12, 16, 16, 16), //
        // Z
        createCuboidShape(0, 0, 0, 4, 4, 16), createCuboidShape(12, 0, 0, 16, 4, 16), //
        createCuboidShape(0, 12, 0, 4, 16, 16), createCuboidShape(12, 12, 0, 16, 16, 16)//
    );

    public MultiPartBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView var1) {
        return new MultiPartBlockEntity();
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, EntityContext ctx) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultiPartBlockEntity) {
            MultiPartBlockEntity container = (MultiPartBlockEntity) be;
            return container.container.getCollisionShape();
        }
        return MISSING_PARTS_SHAPE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, EntityContext ctx) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultiPartBlockEntity) {
            MultiPartBlockEntity container = (MultiPartBlockEntity) be;

            if (LibMultiPart.isDrawingBlockOutlines.getAsBoolean()) {
                Vec3d hitVec = MinecraftClient.getInstance().hitResult.getPos();
                return getPartShape(state, (World) view, pos, hitVec);
            }

            return container.container.getDynamicShape(LibMultiPart.partialTickGetter.getAsFloat());
        }
        return MISSING_PARTS_SHAPE;
    }

    @Override
    public void onBlockRemoved(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean bool) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultiPartBlockEntity) {
            MultiPartBlockEntity container = (MultiPartBlockEntity) be;
            container.onRemoved();
        }
        super.onBlockRemoved(oldState, world, pos, newState, bool);
    }

    @Override
    public void addAllAttributes(World world, BlockPos pos, BlockState state, AttributeList<?> to) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultiPartBlockEntity) {
            MultiPartBlockEntity container = (MultiPartBlockEntity) be;
            container.addAllAttributes(to);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public ItemStack getPickStack(BlockView view, BlockPos pos, BlockState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (view != null && view == mc.world) {
            HitResult hit = mc.hitResult;
            TransientPartIdentifier target = getTargetedMultipart(state, (World) view, pos, hit.getPos());
            if (target != null) {
                return target.part.getPickStack();
            }
        }
        return ItemStack.EMPTY;
    }

    // IBlockMultipart

    @Override
    public Class<TransientPartIdentifier> getKeyClass() {
        return TransientPartIdentifier.class;
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player,
        TransientPartIdentifier subpart) {
        onBlockBreakStart(state, world, pos, player);
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView view, BlockPos pos,
        TransientPartIdentifier subpart) {

        // TODO: More/less expensive depending on the part hit?
        return calcBlockBreakingDelta(state, player, view, pos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player,
        TransientPartIdentifier subpart) {

        onBreak(world, pos, state, player);
    }

    @Override
    public boolean clearBlockState(World world, BlockPos pos, TransientPartIdentifier subpart) {

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultiPartBlockEntity) {
            MultiPartBlockEntity multi = (MultiPartBlockEntity) be;
            return multi.container.removePart(subpart.part);
        }
        return world.clearBlockState(pos, false);
    }

    @Override
    public void onBroken(IWorld world, BlockPos pos, BlockState state, TransientPartIdentifier subpart) {
        onBroken(world, pos, state);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be,
        ItemStack stack, TransientPartIdentifier subpart) {

        DefaultedList<ItemStack> drops = DefaultedList.create();
        subpart.part.addDrops(drops);
        ItemScatterer.spawn(world, pos, drops);
    }

    @Override
    public TransientPartIdentifier getTargetedMultipart(BlockState state, World world, BlockPos pos, Vec3d vec) {

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultiPartBlockEntity) {
            MultiPartBlockEntity multi = (MultiPartBlockEntity) be;

            vec = vec.subtract(new Vec3d(pos));
            float partialTicks = LibMultiPart.partialTickGetter.getAsFloat();

            for (PartHolder holder : multi.container.parts) {

                AbstractPart part = holder.getPart();
                // TODO: Allow for parts made up of sub-parts!

                VoxelShape shape = part.getDynamicShape(partialTicks);
                for (BoundingBox box : shape.getBoundingBoxes()) {
                    if (box.expand(0.01).contains(vec)) {
                        return new TransientPartIdentifier(part);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public VoxelShape getPartShape(BlockState state, World world, BlockPos pos, Vec3d hitVec) {
        TransientPartIdentifier target = getTargetedMultipart(state, world, pos, hitVec);
        if (target == null) {
            return VoxelShapes.empty();
        }
        return target.part.getDynamicShape(LibMultiPart.partialTickGetter.getAsFloat());
    }
}
