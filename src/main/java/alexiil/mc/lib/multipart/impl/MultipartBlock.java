/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateFactory.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeProvider;
import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.event.PartEventEntityCollide;
import alexiil.mc.lib.multipart.api.property.MultipartProperties;
import alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer;
import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;

public class MultipartBlock extends Block
    implements BlockEntityProvider, IBlockMultipart<TransientPartIdentifier>, AttributeProvider, Waterloggable {

    public static final IntProperty LUMINANCE = IntProperty.of("luminance", 0, 15);
    public static final BooleanProperty EMITS_REDSTONE = BooleanProperty.of("emits_redstone");

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

    public MultipartBlock(Settings settings) {
        super(settings);
        setDefaultState(
            getDefaultState()//
                .with(LUMINANCE, 0)//
                .with(EMITS_REDSTONE, false)//
                .with(Properties.WATERLOGGED, false)//
        );
    }

    // ###############
    //
    // Misc
    //
    // ###############

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LUMINANCE, EMITS_REDSTONE, Properties.WATERLOGGED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView var1) {
        return new MultipartBlockEntity();
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getLuminance(BlockState state) {
        return state.get(LUMINANCE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, EntityContext ctx) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            return container.container.getCollisionShape();
        }
        return MISSING_PARTS_SHAPE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, EntityContext ctx) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;

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
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            container.onRemoved();
        }
        super.onBlockRemoved(oldState, world, pos, newState, bool);
    }

    @Override
    public void addAllAttributes(World world, BlockPos pos, BlockState state, AttributeList<?> to) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            container.addAllAttributes(to);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos thisPos, Block otherBlock, BlockPos otherPos,
        boolean unknownBoolean) {
        super.neighborUpdate(state, world, thisPos, otherBlock, otherPos, unknownBoolean);
        BlockEntity be = world.getBlockEntity(thisPos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            container.onNeighbourUpdate(otherPos);
        }
    }

    @Override
    public boolean activate(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
        BlockHitResult hit) {
        boolean handled = super.activate(state, world, pos, player, hand, hit);
        TransientPartIdentifier target = getTargetedMultipart(state, world, pos, hit.getPos());
        if (target != null) {
            handled |= target.part.onActivate(player, hand, hit);
        }
        return handled;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, world, pos, entity);
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            container.container.fireEvent(new PartEventEntityCollide(entity));
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

    // ###############
    //
    // Redstone
    //
    // ###############

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return state.get(EMITS_REDSTONE);
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView view, BlockPos pos, Direction oppositeFace) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            return ((MultipartBlockEntity) be).container.getProperties().getValue(
                MultipartProperties.getStrongRedstonePower(oppositeFace.getOpposite())
            );
        }
        return 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView view, BlockPos pos, Direction oppositeFace) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartPropertyContainer properties = ((MultipartBlockEntity) be).container.getProperties();
            return Math.max(
                properties.getValue(MultipartProperties.getStrongRedstonePower(oppositeFace.getOpposite())), //
                properties.getValue(MultipartProperties.getWeakRedstonePower(oppositeFace.getOpposite()))
            );
        }
        return 0;
    }

    // ###############
    //
    // Waterloggable
    //
    // ###############

    @Override
    public boolean canFillWithFluid(BlockView view, BlockPos pos, BlockState state, Fluid fluid) {
        if (fluid != Fluids.WATER) {
            return false;
        }
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            return ((MultipartBlockEntity) be).container.properties.getValue(MultipartProperties.CAN_BE_WATERLOGGED);
        }
        return true;
    }

    @Override
    public boolean tryFillWithFluid(IWorld world, BlockPos pos, BlockState state, FluidState fluid) {
        if (!canFillWithFluid(world, pos, state, fluid.getFluid())) {
            return false;
        }
        return Waterloggable.super.tryFillWithFluid(world, pos, state, fluid);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    // ###############
    //
    // IBlockMultipart
    //
    // ###############

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
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity multi = (MultipartBlockEntity) be;
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

        DefaultedList<ItemStack> drops = DefaultedList.of();
        subpart.part.addDrops(drops);
        for (AbstractPart additional : subpart.additional) {
            additional.addDrops(drops);
        }
        ItemScatterer.spawn(world, pos, drops);
    }

    @Override
    public TransientPartIdentifier getTargetedMultipart(BlockState state, World world, BlockPos pos, Vec3d vec) {

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity multi = (MultipartBlockEntity) be;

            vec = vec.subtract(new Vec3d(pos));
            float partialTicks = LibMultiPart.partialTickGetter.getAsFloat();

            for (PartHolder holder : multi.container.parts) {

                AbstractPart part = holder.getPart();
                // TODO: Allow for parts made up of sub-parts!

                VoxelShape shape = part.getDynamicShape(partialTicks);
                for (Box box : shape.getBoundingBoxes()) {
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
