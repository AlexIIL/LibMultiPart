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
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultiPartContainer;
import alexiil.mc.lib.multipart.api.MultiPartEventBus;
import alexiil.mc.lib.multipart.api.MultiPartHolder;
import alexiil.mc.lib.multipart.api.event.PartAddedEvent;
import alexiil.mc.lib.multipart.api.event.PartContainerState;
import alexiil.mc.lib.multipart.api.event.PartOfferedEvent;
import alexiil.mc.lib.multipart.api.event.PartRemovedEvent;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import alexiil.mc.lib.multipart.impl.SimpleEventBus.SingleListener;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.NetIdTyped;
import alexiil.mc.lib.net.ParentNetIdDuel;
import alexiil.mc.lib.net.ParentNetIdSingle;

public class PartContainer implements MultiPartContainer {

    static final ParentNetIdSingle<PartContainer> NET_KEY;
    static final NetIdDataK<PartContainer> NET_ID_INITIAL_RENDER_DATA;
    static final NetIdDataK<PartContainer> NET_ID_ADD_PART;
    static final NetIdDataK<PartContainer> NET_ID_REMOVE_PART;
    static final NetIdSignalK<PartContainer> NET_SIGNAL_REDRAW;
    public static final ParentNetIdSingle<AbstractPart> NET_KEY_PART;

    static PartHolder extractPartHolder(AbstractPart value) {
        MultiPartHolder holder = value.holder;
        if (holder == null || holder instanceof PartHolder) {
            return (PartHolder) holder;
        }
        throw new IllegalStateException(
            "Found an AbstractPart that doesn't have the correct class for it's holder! It should be using "
                + PartHolder.class.getName() + ", but instead it's using " + holder.getClass().getName() + "!"
        );
    }

    static {
        NET_KEY = MultiPartBlockEntity.NET_KEY.extractor(
            PartContainer.class, "container", c -> c.blockEntity, b -> b.container
        );
        NET_ID_ADD_PART = NET_KEY.idData("add_part").setReceiver(PartContainer::readAddPart);
        NET_ID_REMOVE_PART = NET_KEY.idData("remove_part").setReceiver(PartContainer::readRemovePart);
        NET_ID_INITIAL_RENDER_DATA = NET_KEY.idData("initial_render_data").setReadWrite(
            PartContainer::readInitialRenderData, PartContainer::writeInitialRenderData
        );
        NET_SIGNAL_REDRAW = NET_KEY.idSignal("redraw").setReceiver(PartContainer::receiveRedraw);
        NET_KEY_PART = new ParentNetIdDuel<PartContainer, AbstractPart>(NET_KEY, "holder", AbstractPart.class) {
            @Override
            protected PartContainer extractParent(AbstractPart value) {
                return extractPartHolder(value).container;
            }

            @Override
            protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, AbstractPart value) {
                PartHolder holder = extractPartHolder(value);
                int index = holder.container.parts.indexOf(holder);
                if (index < 0) {
                    throw new IllegalStateException(
                        "The part " + value + " doesn't have a valid index in it's container!"
                    );
                }
                buffer.writeByte(index);
            }

            @Override
            protected AbstractPart readContext(NetByteBuf buffer, IMsgReadCtx ctx, PartContainer parentValue)
                throws InvalidInputDataException {

                int index = buffer.readUnsignedByte();
                List<PartHolder> parts = parentValue.parts;
                if (index >= parts.size()) {
                    throw new InvalidInputDataException(
                        ("The client is aware of " + parts.size() + " parts, ")
                            + ("but the server has sent data for the " + index + " part!")
                    );
                }
                return parts.get(index).part;
            }
        };
    }

    public final SimpleEventBus eventBus = new SimpleEventBus(this);
    public final List<PartHolder> parts = new ArrayList<>();

    MultiPartBlockEntity blockEntity;
    VoxelShape cachedShape = null;
    VoxelShape cachedCollisionShape = null;

    ImmutableList<PartModelKey> partModelKeys = ImmutableList.of();

    public PartContainer(MultiPartBlockEntity blockEntity) {
        assert blockEntity != null : "The given blockEntity was null!";
        this.blockEntity = blockEntity;
    }

    // MultiPartContainer

    @Override
    public World getMultiPartWorld() {
        return blockEntity.getWorld();
    }

    @Override
    public BlockPos getMultiPartPos() {
        return blockEntity.getPos();
    }

    @Override
    public BlockEntity getMultiPartBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean canPlayerInteract(PlayerEntity player) {
        return blockEntity.canPlayerInteract(player);
    }

    @Override
    public BlockEntity getNeighbourBlockEntity(Direction dir) {
        return getMultiPartWorld().getBlockEntity(getMultiPartPos().offset(dir));
    }

    @Override
    public List<AbstractPart> getAllParts() {
        List<AbstractPart> list = new ArrayList<>();
        for (PartHolder holder : parts) {
            list.add(holder.part);
        }
        return list;
    }

    @Override
    public IPartOffer offerNewPart(Function<MultiPartHolder, AbstractPart> creator) {
        PartHolder holder = new PartHolder(this, creator);
        if (!canAdd(holder)) {
            return null;
        }
        return new IPartOffer() {
            @Override
            public MultiPartHolder getHolder() {
                return holder;
            }

            @Override
            public void apply() {
                // TODO: Throw an error if the state changed!
                addPartInternal(holder);
            }
        };
    }

    @Override
    public MultiPartHolder addNewPart(Function<MultiPartHolder, AbstractPart> creator) {
        PartHolder holder = new PartHolder(this, creator);
        if (!canAdd(holder)) {
            return null;
        }
        addPartInternal(holder);
        return holder;
    }

    private boolean canAdd(PartHolder offered) {
        VoxelShape currentShape = getCurrentShape();
        for (PartHolder holder : parts) {
            AbstractPart part = holder.part;

            VoxelShape shapeOther = part.getShape();
            VoxelShape shapeOffered = offered.part.getShape();

            // Basic overlap checking
            if (!VoxelShapes.matchesAnywhere(shapeOther, shapeOffered, BooleanBiFunction.AND)) {
                continue;
            }

            // Complete containment checking
            if (!VoxelShapes.matchesAnywhere(currentShape, shapeOffered, BooleanBiFunction.ONLY_SECOND)) {
                return false;
            }

            VoxelShape leftoverShape = shapeOther;
            for (PartHolder h2 : parts) {
                if (h2.part != part) {
                    VoxelShape h2shape = h2.part.getShape();
                    if (!h2shape.getBoundingBox().intersects(leftoverShape.getBoundingBox())) {
                        continue;
                    }
                    leftoverShape = VoxelShapes.combine(leftoverShape, h2shape, BooleanBiFunction.ONLY_FIRST);
                    if (leftoverShape.isEmpty()) {
                        return false;
                    }
                }
            }
            if (!VoxelShapes.matchesAnywhere(leftoverShape, shapeOffered, BooleanBiFunction.ONLY_FIRST)) {
                return false;
            }

            // Check with each part for overlaps
            if (!part.canOverlapWith(offered.part) || !offered.part.canOverlapWith(part)) {
                return false;
            }
        }

        PartOfferedEvent event = new PartOfferedEvent(offered.part);
        eventBus.fireEvent(event);
        return event.isAllowed();
    }

    void addPartInternal(PartHolder holder) {
        parts.add(holder);
        holder.part.onAdded(eventBus);
        recalculateShape();
        eventBus.fireEvent(new PartAddedEvent(holder.part));
        sendNetworkUpdate(PartContainer.this, NET_ID_ADD_PART, (p, buffer, ctx) -> {
            holder.writeCreation(buffer, ctx);
        });
    }

    private void readAddPart(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();
        PartHolder holder = new PartHolder(this, buffer, ctx);
        assert holder.part != null;
        parts.add(holder);
        holder.part.onAdded(eventBus);
        eventBus.fireEvent(new PartAddedEvent(holder.part));
        recalculateShape();
        redrawIfChanged();
    }

    @Override
    public boolean removePart(AbstractPart part) {
        for (int i = 0; i < parts.size(); i++) {
            PartHolder holder = parts.get(i);
            if (holder.part == part) {
                if (blockEntity.isServerWorld()) {
                    holder.part.onRemoved();
                    parts.remove(i);
                    eventBus.removeListeners(part);
                    final int index = i;
                    eventBus.fireEvent(new PartRemovedEvent(part));
                    part.onRemoved();
                    if (parts.isEmpty()) {
                        // TODO: Waterlogging!
                        blockEntity.world().setBlockState(getMultiPartPos(), Blocks.AIR.getDefaultState());
                    } else {
                        sendNetworkUpdate(this, NET_ID_REMOVE_PART, (p, buffer, ctx) -> {
                            buffer.writeByte(index);
                        });
                        recalculateShape();
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void readRemovePart(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();

        int index = buffer.readUnsignedByte();
        if (index >= parts.size()) {
            throw new InvalidInputDataException("Invalid pluggable index " + index + " - we've probably got desynced!");
        }
        PartHolder removed = parts.remove(index);
        eventBus.fireEvent(new PartRemovedEvent(removed.part));
        eventBus.removeListeners(removed.part);
        removed.part.onRemoved();
        recalculateShape();
        redrawIfChanged();
    }

    @Override
    public VoxelShape getCurrentShape() {
        if (cachedShape == null) {
            cachedShape = VoxelShapes.empty();
            for (PartHolder holder : parts) {
                cachedShape = VoxelShapes.union(cachedShape, holder.part.getShape());
            }
        }
        return cachedShape;
    }

    @Override
    public VoxelShape getCollisionShape() {
        if (cachedCollisionShape == null) {
            cachedCollisionShape = VoxelShapes.empty();
            for (PartHolder holder : parts) {
                cachedCollisionShape = VoxelShapes.union(cachedCollisionShape, holder.part.getCollisionShape());
            }
            if (parts.isEmpty()) {
                cachedCollisionShape = MultiPartBlock.MISSING_PARTS_SHAPE;
            }
        }
        return cachedCollisionShape;
    }

    @Override
    public VoxelShape getDynamicShape(float partialTicks) {
        VoxelShape shape = VoxelShapes.empty();
        for (PartHolder holder : parts) {
            shape = VoxelShapes.union(shape, holder.part.getDynamicShape(partialTicks));
        }
        return shape;
    }

    @Override
    public void recalculateShape() {
        cachedShape = null;
        cachedCollisionShape = null;
    }

    @Override
    public void redrawIfChanged() {
        ImmutableList.Builder<PartModelKey> builder = ImmutableList.builder();
        for (PartHolder holder : parts) {
            PartModelKey key = holder.part.getModelKey();
            if (key != null) {
                builder.add(key);
            }
        }
        ImmutableList<PartModelKey> list = builder.build();
        if (list.equals(partModelKeys)) {
            return;
        }
        partModelKeys = list;
        if (isClientWorld()) {
            blockEntity.world().scheduleBlockRender(blockEntity.getPos());
        } else {
            sendNetworkUpdate(this, NET_SIGNAL_REDRAW);
        }
    }

    private void receiveRedraw(IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();
        redrawIfChanged();
    }

    public ImmutableList<PartModelKey> getPartModelKeys() {
        return partModelKeys;
    }

    private void writeInitialRenderData(NetByteBuf buffer, IMsgWriteCtx ctx) {
        buffer.writeByte(parts.size());
        for (PartHolder holder : parts) {
            holder.writeCreation(buffer, ctx);
        }
    }

    private void readInitialRenderData(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        int count = buffer.readUnsignedByte();
        for (int i = 0; i < count; i++) {
            PartHolder holder = new PartHolder(this, buffer, ctx);
            parts.add(holder);
            holder.part.onAdded(eventBus);
        }
        validate();
        recalculateShape();
        redrawIfChanged();
    }

    @Override
    public <T> void sendNetworkUpdate(T obj, NetIdTyped<T> netId) {
        blockEntity.sendNetworkUpdate(obj, netId);
    }

    @Override
    public <T> void sendNetworkUpdate(T obj, NetIdDataK<T> netId, IMsgDataWriterK<T> writer) {
        blockEntity.sendNetworkUpdate(obj, netId, writer);
    }

    @Override
    public MultiPartEventBus getEventBus() {
        return eventBus;
    }

    // Internals

    void fromTag(CompoundTag tag) {
        ListTag allPartsTag = tag.getList("parts", new CompoundTag().getType());
        for (int i = 0; i < allPartsTag.size(); i++) {
            CompoundTag partTag = allPartsTag.getCompoundTag(i);
            PartHolder holder = new PartHolder(this, partTag);
            if (holder.part != null) {
                parts.add(holder);
            }
        }
    }

    CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag partsTag = new ListTag();
        for (PartHolder part : parts) {
            partsTag.add(part.toTag());
        }
        tag.put("parts", partsTag);
        return tag;
    }

    void validate() {
        eventBus.clearListeners();
        for (PartHolder holder : parts) {
            holder.part.onAdded(eventBus);
        }
        eventBus.fireEvent(PartContainerState.VALIDATE);
    }

    void invalidate() {
        eventBus.fireEvent(PartContainerState.INVALIDATE);
    }

    void onListenerAdded(SingleListener<?> single) {
        // This was removed because minecraft doesn't seem to like removing a block entity while it's being ticked.

        // if (single.clazz == PartTickEvent.class && !(blockEntity instanceof Tickable)) {
        // World world = blockEntity.world();
        // if (world.getBlockEntity(getMultiPartPos()) == blockEntity) {
        // world.setBlockEntity(getMultiPartPos(), new MultiPartBlockEntity.Ticking(this));
        // LibMultiPart.LOGGER.info("Switching " + getMultiPartPos() + " from non-ticking to ticking.");
        // } else {
        // LibMultiPart.LOGGER.info("Failed to switch " + getMultiPartPos() + " from non-ticking to ticking!");
        // }
        // }
    }

    void onListenerRemoved(SingleListener<?> single) {
        // Nothing needs to happen quite yet
    }

    void addAllAttributes(AttributeList<?> list) {
        list.offer(this);
        for (PartHolder holder : parts) {
            holder.part.addAllAttributes(list);
        }
    }
}
