/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.List;

import javax.annotation.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
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
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
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
import alexiil.mc.lib.multipart.api.PartLootParams;
import alexiil.mc.lib.multipart.api.PartLootParams.BrokenSinglePart;
import alexiil.mc.lib.multipart.api.SubdividedPart;
import alexiil.mc.lib.multipart.api.event.PartEventEntityCollide;
import alexiil.mc.lib.multipart.api.property.MultipartProperties;
import alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier.IdAdditional;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier.IdSubPart;
import alexiil.mc.lib.multipart.mixin.api.IBlockCustomParticles;
import alexiil.mc.lib.multipart.mixin.api.IBlockDynamicCull;
import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;

public class MultipartBlock extends Block
    implements BlockEntityProvider, IBlockMultipart<TransientPartIdentifier>, AttributeProvider, Waterloggable,
    IBlockDynamicCull, IBlockCustomParticles
{
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LUMINANCE, EMITS_REDSTONE, Properties.WATERLOGGED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView var1) {
        return new MultipartBlockEntity();
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
                Vec3d hitVec = MinecraftClient.getInstance().crosshairTarget.getPos();
                return getPartOutlineShape(state, (World) view, pos, hitVec);
            }

            return container.container.getOutlineShape();
        }
        return MISSING_PARTS_SHAPE;
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView view, BlockPos pos) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            return container.container.getCullingShape();
        }
        return super.getCullingShape(state, view, pos);
    }

    @Override
    public boolean hasDynamicCull(BlockState state) {
        return true;
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
    public void neighborUpdate(
        BlockState state, World world, BlockPos thisPos, Block otherBlock, BlockPos otherPos, boolean unknownBoolean
    ) {
        super.neighborUpdate(state, world, thisPos, otherBlock, otherPos, unknownBoolean);
        BlockEntity be = world.getBlockEntity(thisPos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            container.onNeighbourUpdate(otherPos);
        }
    }

    @Override
    public ActionResult onUse(
        BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit
    ) {
        ActionResult handled = super.onUse(state, world, pos, player, hand, hit);
        TransientPartIdentifier target = getTargetedMultipart(state, world, pos, hit.getPos());
        if (target != null) {
            handled = target.part.onUse(player, hand, hit);
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
        HitResult hit = mc.crosshairTarget;
        if (view != null && view == mc.world && hit != null) {
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
            return ((MultipartBlockEntity) be).container.getProperties()
                .getValue(MultipartProperties.getStrongRedstonePower(oppositeFace.getOpposite()));
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
    // IBlockCustomParticles
    //
    // ###############

    @Override
    @Environment(EnvType.CLIENT)
    public boolean spawnBreakingParticles(World world, BlockPos pos, BlockState state, Direction side, Vec3d hitVec) {
        TransientPartIdentifier target = getTargetedMultipart(state, world, pos, hitVec);
        if (target != null) {

            if (target.extra instanceof IdSubPart<?>) {
                if (spawnSubBreakingParticles(hitVec, side, (IdSubPart<?>) target.extra)) {
                    return true;
                }
            } else if (target.extra instanceof IdAdditional) {
                for (AbstractPart extra : ((IdAdditional) target.extra).additional) {
                    extra.spawnBreakingParticles(side);
                }
            }

            return target.part.spawnBreakingParticles(side);
        }
        return false;
    }

    private static <Sub> boolean spawnSubBreakingParticles(Vec3d hitVec, Direction side, IdSubPart<Sub> extra) {
        return extra.part.spawnBreakingParticles(hitVec, extra.subpart, side);
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
    public void onBlockBreakStart(
        BlockState state, World world, BlockPos pos, PlayerEntity player, TransientPartIdentifier subpart
    ) {
        onBlockBreakStart(state, world, pos, player);
    }

    @Override
    public float calcBlockBreakingDelta(
        BlockState state, PlayerEntity player, BlockView view, BlockPos pos, TransientPartIdentifier subpart
    ) {
        return subpart.part.calculateBreakingDelta(player);
    }

    @Override
    public void onBreak(
        World world, BlockPos pos, BlockState state, PlayerEntity player, TransientPartIdentifier subpart
    ) {
        if (subpart.extra instanceof IdSubPart<?>) {
            if (onSubpartBreak(player, (IdSubPart<?>) subpart.extra)) {
                return;
            }
        } else if (subpart.extra instanceof IdAdditional) {
            for (AbstractPart part : ((IdAdditional) subpart.extra).additional) {
                part.onBreak(player);
            }
        }
        if (!subpart.part.onBreak(player)) {
            onBreak(world, pos, state, player);
        }
    }

    private static <Sub> boolean onSubpartBreak(PlayerEntity player, IdSubPart<Sub> extra) {
        return extra.part.onSubpartBreak(player, extra.subpart);
    }

    @Override
    public boolean clearBlockState(World world, BlockPos pos, TransientPartIdentifier subpart) {

        if (subpart.extra instanceof IdSubPart<?>) {
            return clearSubPart((IdSubPart<?>) subpart.extra);
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity multi = (MultipartBlockEntity) be;
            return multi.container.removePart(subpart.part);
        }
        return world.removeBlock(pos, false);
    }

    private static <Sub> boolean clearSubPart(IdSubPart<Sub> extra) {
        return extra.part.clearSubpart(extra.subpart);
    }

    @Override
    public void onBroken(IWorld world, BlockPos pos, BlockState state, TransientPartIdentifier subpart) {
        onBroken(world, pos, state);
    }

    @Override
    public void afterBreak(
        World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack,
        TransientPartIdentifier subpart
    ) {
        try {
            droppingPart = subpart;
            super.afterBreak(world, player, pos, state, blockEntity, stack);
        } finally {
            droppingPart = null;
        }
        super.afterBreak(world, player, pos, state, blockEntity, stack);
        DefaultedList<ItemStack> drops = DefaultedList.of();
        if (subpart.extra instanceof IdSubPart<?>) {
            afterSubpartBreak(player, stack, drops, (IdSubPart<?>) subpart.extra);
        } else {
            subpart.part.addDrops(drops);
            for (AbstractPart additional : ((IdAdditional) subpart.extra).additional) {
                additional.addDrops(drops);
            }
        }
        ItemScatterer.spawn(world, pos, drops);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        DefaultedList<ItemStack> drops = DefaultedList.of();
        BlockEntity be = builder.get(LootContextParameters.BLOCK_ENTITY);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity mpbe = (MultipartBlockEntity) be;
            for (PartHolder holder : mpbe.container.parts) {
                
            }
        }
        return drops;

    }

    private static <Sub> void afterSubpartBreak(
        PlayerEntity player, ItemStack tool, DefaultedList<ItemStack> drops, IdSubPart<Sub> extra
    ) {
        extra.part.afterSubpartBreak(player, tool, drops, extra.subpart);
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

                if (part instanceof SubdividedPart<?>) {
                    TransientPartIdentifier id = getTargetSubPart((SubdividedPart<?>) part, vec);
                    if (id != null) {
                        return id;
                    }
                }

                VoxelShape shape = part.getOutlineShape();
                for (Box box : shape.getBoundingBoxes()) {
                    if (box.expand(0.01).contains(vec)) {
                        return new TransientPartIdentifier(part);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static <Sub> TransientPartIdentifier getTargetSubPart(SubdividedPart<Sub> part, Vec3d vec) {
        Sub subpart = part.getTargetedSubpart(vec);
        if (subpart != null) {
            return new TransientPartIdentifier(part, subpart);
        }
        return null;
    }

    @Override
    public VoxelShape getPartOutlineShape(BlockState state, World world, BlockPos pos, Vec3d hitVec) {
        TransientPartIdentifier target = getTargetedMultipart(state, world, pos, hitVec);
        if (target == null) {
            return VoxelShapes.empty();
        }
        float partialTicks = LibMultiPart.partialTickGetter.getAsFloat();
        if (target.extra instanceof IdSubPart<?>) {
            VoxelShape sub = getSubpartShape((IdSubPart<?>) target.extra, hitVec, partialTicks);
            if (sub != null) {
                return sub;
            }
        }
        return target.part.getDynamicShape(partialTicks);
    }

    @Nullable
    private static <S> VoxelShape getSubpartShape(IdSubPart<S> sub, Vec3d vec, float partialTicks) {
        return sub.part.getSubpartDynamicShape(vec, sub.subpart, partialTicks);
    }
}
