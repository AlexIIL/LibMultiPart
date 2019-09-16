/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.MultipartContainer.PartOffer;
import alexiil.mc.lib.multipart.api.MultipartHolder;
import alexiil.mc.lib.multipart.api.MultipartUtil;
import alexiil.mc.lib.multipart.api.NativeMultipart;

/** Contains the backend for {@link MultipartUtil}. */
public final class MultipartUtilImpl {

    @Nullable
    public static PartContainer get(World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            return ((MultipartBlockEntity) be).container;
        }
        return null;
    }

    @Nullable
    public static MultipartContainer turnIntoMultipart(World world, BlockPos pos) {
        // See if there's an existing multipart that doesn't need to be changed
        PartContainer existing = get(world, pos);
        if (existing != null) {
            return existing;
        }

        Fluid fluid = world.getFluidState(pos).getFluid();
        boolean hasWater = fluid == Fluids.WATER;
        if (fluid != Fluids.WATER && fluid != Fluids.EMPTY) {
            return null;
        }

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof NativeMultipart) {
            NativeMultipart nativeBlock = (NativeMultipart) state.getBlock();
            List<MultipartCreator> conversions = nativeBlock.getMultipartConversion(world, pos, state);
            if (conversions == null) {
                return null;
            }
            if (!conversions.isEmpty()) {
                PartOffer offer = offerAdder(world, pos, hasWater, conversions, null);
                if (offer == null) {
                    return null;
                }
                offer.apply();
                return offer.getHolder().getContainer();
            }
        }
        return null;
    }

    @Nullable
    public static PartOffer offerNewPart(World world, BlockPos pos, MultipartCreator creator) {

        // See if there's an existing multipart that we can add to
        PartContainer currentContainer = get(world, pos);
        if (currentContainer != null) {
            return currentContainer.offerNewPart(creator);
        }

        Fluid fluid = world.getFluidState(pos).getFluid();
        boolean hasWater = fluid == Fluids.WATER;
        if (fluid != Fluids.WATER && fluid != Fluids.EMPTY) {
            return null;
        }

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof NativeMultipart) {
            NativeMultipart nativeBlock = (NativeMultipart) state.getBlock();
            List<MultipartCreator> conversions = nativeBlock.getMultipartConversion(world, pos, state);
            if (conversions == null) {
                return null;
            }
            if (!conversions.isEmpty()) {
                return offerAdder(world, pos, hasWater, conversions, creator);
            }
        } else if (!state.isAir()) {
            return null;
        }

        MultipartBlockEntity be = new MultipartBlockEntity();
        be.setWorld(world);
        be.setPos(pos);
        PartContainer container = new PartContainer(be);
        PartHolder holder = new PartHolder(container, creator);
        VoxelShape collisionShape = holder.part.getCollisionShape();

        if (!world.intersectsEntities(null, collisionShape)) {
            return null;
        }
        return new PartOffer() {
            @Override
            public MultipartHolder getHolder() {
                return holder;
            }

            @Override
            public void apply() {
                BlockState newState = LibMultiPart.BLOCK.getDefaultState();
                newState = newState.with(Properties.WATERLOGGED, hasWater);
                world.setBlockState(pos, newState);
                MultipartBlockEntity newBe = (MultipartBlockEntity) world.getBlockEntity(pos);
                assert newBe != null;
                newBe.container = container;
                container.blockEntity = newBe;
                container.addPartInternal(holder);
            }
        };
    }

    @Nullable
    private static PartOffer offerAdder(World world, BlockPos pos, boolean hasWater, List<MultipartCreator> existing,
        MultipartCreator creatorB) {

        MultipartBlockEntity be = new MultipartBlockEntity();
        be.setWorld(world);
        be.setPos(pos);
        PartContainer container = new PartContainer(be);

        List<PartHolder> existingHolders = new ArrayList<>();
        for (MultipartCreator creator : existing) {
            PartHolder holder = new PartHolder(container, creator);
            existingHolders.add(holder);

            // Add the existing ones so that they can intercept the offered part
            container.parts.add(holder);

            if (!world.intersectsEntities(null, holder.part.getCollisionShape())) {
                return null;
            }
        }

        PartHolder offeredHolder = creatorB == null ? null : new PartHolder(container, creatorB);
        VoxelShape offeredShape = offeredHolder == null ? null : offeredHolder.part.getCollisionShape();

        if (offeredHolder != null) {
            if (!world.intersectsEntities(null, offeredShape)) {
                return null;
            }

            container.recalculateShape();

            if (!container.canAdd(offeredHolder)) {
                return null;
            }
        }

        return new PartOffer() {
            @Override
            public MultipartHolder getHolder() {
                return offeredHolder;
            }

            @Override
            public void apply() {
                // Cleanup the temporary additions
                container.parts.clear();

                // Actually place the new multipart
                BlockState newState = LibMultiPart.BLOCK.getDefaultState();
                newState = newState.with(Properties.WATERLOGGED, hasWater);
                world.setBlockState(pos, newState);
                MultipartBlockEntity newBe = (MultipartBlockEntity) world.getBlockEntity(pos);
                assert newBe != null;
                newBe.container = container;
                container.blockEntity = newBe;
                for (PartHolder holder : existingHolders) {
                    container.addPartInternal(holder);
                }
                if (offeredHolder != null) {
                    container.addPartInternal(offeredHolder);
                }
            }
        };
    }
}
