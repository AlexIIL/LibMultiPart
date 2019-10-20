/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.event.EventListener;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import alexiil.mc.lib.multipart.impl.PartContainer;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.NetIdTyped;
import alexiil.mc.lib.net.ParentNetIdSingle;

/** The base class for every part in a multipart block. */
public abstract class AbstractPart {
    public static final ParentNetIdSingle<AbstractPart> NET_ID;
    public static final NetIdDataK<AbstractPart> NET_RENDER_DATA;

    static {
        NET_ID = PartContainer.NET_KEY_PART;
        NET_RENDER_DATA = NET_ID.idData("render_data").setReadWrite(
            AbstractPart::readRenderData, AbstractPart::writeRenderData
        );
    }

    public final PartDefinition definition;
    public final MultipartHolder holder;

    public AbstractPart(PartDefinition definition, MultipartHolder holder) {
        this.definition = definition;
        this.holder = holder;
    }

    public CompoundTag toTag() {
        return new CompoundTag();
    }

    /** Writes the payload that will be passed into
     * {@link PartDefinition#loadFromBuffer(MultipartHolder, NetByteBuf, IMsgReadCtx)} on the client. (This is called on
     * the server and sent to the client). Note that this will be called *instead* of write and read payload.
     * 
     * @param ctx TODO */
    public void writeCreationData(NetByteBuf buffer, IMsgWriteCtx ctx) {
        ctx.assertServerSide();
    }

    public void writeRenderData(NetByteBuf buffer, IMsgWriteCtx ctx) {
        ctx.assertServerSide();
    }

    public void readRenderData(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();
    }

    /** Sends the given {@link NetIdDataK} or {@link NetIdSignalK} to every player currently watching this multipart. */
    public final <T> void sendNetworkUpdate(T obj, NetIdTyped<T> netId) {
        holder.getContainer().sendNetworkUpdate(obj, netId);
    }

    /** Sends the given {@link NetIdDataK} to every player currently watching this multipart, with a custom
     * {@link IMsgDataWriterK}. */
    public final <T> void sendNetworkUpdate(T obj, NetIdDataK<T> netId, IMsgDataWriterK<T> writer) {
        holder.getContainer().sendNetworkUpdate(obj, netId, writer);
    }

    /** Called whenever this part was added to the {@link MultipartContainer}, either in {@link BlockEntity#validate()}
     * or when it is manually added by an item.
     * <p>
     * Register event handlers (as methods) with {@link MultipartEventBus#addListener(Object, Class, EventListener)}.
     * 
     * @param bus The event bus to register with. This is shorthand for
     *            <code>holder.getContainer().getEventBus()</code> */
    public void onAdded(MultipartEventBus bus) {
        // Nothing registers by default.
    }

    public void onRemoved() {
        // Nothing to do by default
    }

    protected final void addRequiredPart(AbstractPart required) {
        holder.addRequiredPart(required);
    }

    protected final void removeRequiredPart(AbstractPart required) {
        holder.removeRequiredPart(required);
    }

    /** @return The {@link VoxelShape} to use for calculating if this pluggable overlaps with another pluggable. */
    public abstract VoxelShape getShape();

    /** Checks to see if this {@link AbstractPart} can overlap with the other part. Note that this is only called if the
     * {@link #getShape()} of this part intersects with the {@link #getShape()} of the other part. However this is never
     * called if the shape of one of the parts is completely contained by the shape of the other part.
     * <p>
     * This is called once for each part currently contained in a {@link MultipartContainer} in
     * {@link MultipartContainer#offerNewPart(MultipartCreator, boolean)}. */
    public boolean canOverlapWith(AbstractPart other) {
        return false;
    }

    /** @return The shape to use when calculating lighting, solidity logic, collisions with entities, ray tracing, etc.
     *         This should always encompass {@link #getShape()}. */
    public VoxelShape getCollisionShape() {
        return getShape();
    }

    /** @return The (potentially dynamic) shape for rendering bounding boxes and ray tracing. */
    public VoxelShape getDynamicShape(float partialTicks) {
        return getCollisionShape();
    }

    /** @return True if this pluggable should be an {@link AttributeList#obstruct(VoxelShape) obstacle} for attributes
     *         with it's {@link #getShape()} when searching in this particular direction. */
    public boolean isBlocking(Direction searchDirection) {
        return true;
    }

    /** Offers every contained attribute to the given attribute list. NOTE: This must always use
     * {@link AttributeList#offer(Object, VoxelShape)} with {@link #getShape()} as the {@link VoxelShape} argument! */
    public void addAllAttributes(AttributeList<?> list) {
        Direction searchDirection = list.getSearchDirection();
        if (searchDirection != null && isBlocking(searchDirection)) {
            list.obstruct(getShape());
        }
    }

    /** Called whenever this part is picked by the player (similar to
     * {@link Block#getPickStack(BlockView, BlockPos, BlockState)})
     * 
     * @return The stack that should be picked, or ItemStack.EMPTY if no stack can be picked from this pluggable. */
    public ItemStack getPickStack() {
        return ItemStack.EMPTY;
    }

    public void addDrops(DefaultedList<ItemStack> to) {
        ItemStack pickStack = getPickStack();
        if (!pickStack.isEmpty()) {
            to.add(pickStack);
        }
    }

    /** Called whenever this part is activated via
     * {@link Block#activate(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult)}. */
    public boolean onActivate(PlayerEntity player, Hand hand, BlockHitResult hit) {
        return false;
    }

    @Nullable
    public abstract PartModelKey getModelKey();
}
