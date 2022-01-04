/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.AbstractPart.ItemDropTarget;
import alexiil.mc.lib.multipart.api.PartLootParams;
import alexiil.mc.lib.multipart.api.SubdividedPart;
import alexiil.mc.lib.multipart.api.event.PartEventEntityCollide;
import alexiil.mc.lib.multipart.api.event.PartPrecipitationTickEvent;
import alexiil.mc.lib.multipart.api.event.PartRandomDisplayTickEvent;
import alexiil.mc.lib.multipart.api.event.PartRandomTickEvent;
import alexiil.mc.lib.multipart.api.event.PartScheduledTickEvent;
import alexiil.mc.lib.multipart.api.property.MultipartProperties;
import alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier.IdAdditional;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier.IdSubPart;
import alexiil.mc.lib.multipart.mixin.api.IBlockCustomParticles;
import alexiil.mc.lib.multipart.mixin.api.IBlockDynamicCull;
import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;

public class MultipartBlock extends Block
    implements BlockEntityProvider, IBlockMultipart<TransientPartIdentifier>, Waterloggable, IBlockDynamicCull,
    IBlockCustomParticles
{
    public static final IntProperty LUMINANCE = IntProperty.of("luminance", 0, 15);
    public static final BooleanProperty EMITS_REDSTONE = BooleanProperty.of("emits_redstone");

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
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MultipartBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        World world, BlockState state, BlockEntityType<T> type
    ) {
        return type == LibMultiPart.BLOCK_ENTITY ? (w, p, s, be) -> ((MultipartBlockEntity) be).tick() : null;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        // This is really just a TODO for the block entity.rotate method
        throw new UnsupportedOperationException("Need BlockEntity.rotate too pls mojang");
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        // This is really just a TODO for the block entity.mirror method
        throw new UnsupportedOperationException("Need BlockEntity.mirror too pls mojang");
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext ctx) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            return container.container.getCollisionShape();
        }
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext ctx) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            return ((MultipartBlockEntity) be).container.getOutlineShape();
        }
        return VoxelShapes.empty();
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
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean bool) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity container = (MultipartBlockEntity) be;
            container.onRemoved();
        }
        super.onStateReplaced(oldState, world, pos, newState, bool);
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
        TransientPartIdentifier target = getMultipartTarget(state, world, pos, hit.getPos());
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
            TransientPartIdentifier target = getMultipartTarget(state, view, pos, hit.getPos());
            if (target != null) {
                return target.part.getPickStack();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity container) {
            container.container.fireEvent(PartRandomTickEvent.INSTANCE);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity container) {
            container.container.fireEvent(PartScheduledTickEvent.INSTANCE);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity container) {
            container.container.fireEvent(PartRandomDisplayTickEvent.INSTANCE);
        }
    }

    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity container) {
            container.container.fireEvent(new PartPrecipitationTickEvent(precipitation));
        }
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
        if (be instanceof MultipartBlockEntity multiBE) {
            return multiBE.container.getStrongRedstonePower(oppositeFace.getOpposite());
        }
        return 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView view, BlockPos pos, Direction oppositeFace) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity multiBE) {
            return multiBE.container.getWeakRedstonePower(oppositeFace.getOpposite());
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
    public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluid) {
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
        TransientPartIdentifier target = getMultipartTarget(state, world, pos, hitVec);
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
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state, TransientPartIdentifier subpart) {
        onBroken(world, pos, state);
    }

    @Override
    public void afterBreak(
        World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack,
        TransientPartIdentifier subpart
    ) {
        if (subpart.extra instanceof IdSubPart<?>) {
            afterSubpartBreak(player, stack, (IdSubPart<?>) subpart.extra);
        } else {
            if (world instanceof ServerWorld) {
                LootContext.Builder ctxBuilder = new LootContext.Builder((ServerWorld) world);
                ctxBuilder.random(world.random);
                ctxBuilder.parameter(LootContextParameters.BLOCK_STATE, state);
                ctxBuilder.parameter(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos));
                ctxBuilder.parameter(LootContextParameters.TOOL, stack);
                ctxBuilder.optionalParameter(LootContextParameters.THIS_ENTITY, player);
                ctxBuilder.optionalParameter(LootContextParameters.BLOCK_ENTITY, blockEntity);
                subpart.putLootContext(ctxBuilder);
                LootContext context = ctxBuilder.build(PartLootParams.PART_TYPE);
                subpart.part.afterBreak(player);
                subpart.part.addDrops(createDropTarget(subpart.part), context);
                for (AbstractPart part : ((IdAdditional) subpart.extra).additional) {
                    part.afterBreak(player);
                    part.addDrops(createDropTarget(part), context);
                }
            } else {
                subpart.part.afterBreak(player);
                for (AbstractPart part : ((IdAdditional) subpart.extra).additional) {
                    part.afterBreak(player);
                }
            }
        }
    }

    private static <Sub> void afterSubpartBreak(PlayerEntity player, ItemStack tool, IdSubPart<Sub> extra) {
        extra.part.afterSubpartBreak(player, tool, extra.subpart);
    }

    private static ItemDropTarget createDropTarget(AbstractPart part) {
        return new ItemDropTarget() {

            Vec3d center = null;

            @Override
            public boolean dropsAsEntity() {
                return true;
            }

            @Override
            public void drop(ItemStack stack) {
                if (center == null) {
                    center = part.getOutlineShape().getBoundingBox().getCenter();
                    center = center.add(Vec3d.of(part.holder.getContainer().getMultipartPos()));
                }
                drop(stack, center);
            }

            @Override
            public void drop(ItemStack stack, Vec3d pos) {
                World world = part.holder.getContainer().getMultipartWorld();
                while (!stack.isEmpty()) {
                    ItemStack split = stack.split(world.random.nextInt(21) + 10);
                    ItemEntity ent = new ItemEntity(world, pos.x, pos.y, pos.z, split);
                    ent.setVelocity(
                        world.random.nextGaussian() * 0.05, //
                        world.random.nextGaussian() * 0.05 + 0.2, //
                        world.random.nextGaussian() * 0.05//
                    );
                    world.spawnEntity(ent);
                }
            }

            @Override
            public void drop(ItemStack stack, Vec3d pos, Vec3d velocity) {
                World world = part.holder.getContainer().getMultipartWorld();
                ItemEntity ent = new ItemEntity(world, pos.x, pos.y, pos.z, stack);
                ent.setVelocity(velocity);
                world.spawnEntity(ent);
            }
        };
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        DefaultedList<ItemStack> drops = DefaultedList.of();
        builder.parameter(LootContextParameters.BLOCK_STATE, state);
        BlockEntity be = builder.get(LootContextParameters.BLOCK_ENTITY);

        LootContext lootContext;
        lootContext = builder.build(LootContextTypes.BLOCK);

        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity mpbe = (MultipartBlockEntity) be;

            ItemDropTarget target = new ItemDropTarget() {
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
            };

            for (PartHolder holder : mpbe.container.parts) {
                holder.part.addDrops(target, lootContext);
            }
        }
        return drops;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean playHitSound(
        World world, BlockPos pos, BlockState state, PlayerEntity player, TransientPartIdentifier subpart
    ) {
        if (subpart.extra instanceof IdSubPart<?>) {
            playSubpartHitSound(player, (IdSubPart<?>) subpart.extra);
        } else {
            subpart.part.playHitSound(player);
        }
        return true;
    }

    private static <T> void playSubpartHitSound(PlayerEntity player, IdSubPart<T> subpart) {
        subpart.part.playHitSound(player, subpart.subpart);
    }

    @Deprecated
    @Override
    public TransientPartIdentifier getTargetedMultipart(BlockState state, World world, BlockPos pos, Vec3d vec) {
        return getMultipartTarget(state, world, pos, vec);
    }

    @Override
    public TransientPartIdentifier getMultipartTarget(BlockState state, BlockView view, BlockPos pos, Vec3d vec) {
        BlockEntity be = view.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            MultipartBlockEntity multi = (MultipartBlockEntity) be;

            vec = vec.subtract(Vec3d.of(pos));

            for (PartHolder holder : multi.container.parts) {

                AbstractPart part = holder.getPart();

                if (part instanceof SubdividedPart<?>) {
                    TransientPartIdentifier id = getTargetSubPart((SubdividedPart<?>) part, vec);
                    if (id != null) {
                        return id;
                    }
                }

                if (doesContain(part, vec)) {
                    return new TransientPartIdentifier(part);
                }
            }
        }

        return null;
    }

    static boolean doesContain(AbstractPart part, Vec3d vec) {
        VoxelShape shape = part.getOutlineShape();
        for (Box box : shape.getBoundingBoxes()) {
            if (box.expand(0.01).contains(vec)) {
                return true;
            }
        }
        return false;
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
    @Deprecated
    public VoxelShape getPartOutlineShape(BlockState state, World world, BlockPos pos, Vec3d hitVec) {
        return getPartOutline(state, world, pos, hitVec);
    }

    @Override
    public VoxelShape getPartOutline(BlockState state, BlockView world, BlockPos pos, Vec3d hitVec) {
        TransientPartIdentifier target = getMultipartTarget(state, world, pos, hitVec);
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
