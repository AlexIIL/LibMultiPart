/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.SystemUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.MultipartEventBus;
import alexiil.mc.lib.multipart.api.MultipartHolder;
import alexiil.mc.lib.multipart.api.event.PartAddedEvent;
import alexiil.mc.lib.multipart.api.event.PartContainerState;
import alexiil.mc.lib.multipart.api.event.PartOfferedEvent;
import alexiil.mc.lib.multipart.api.event.PartRemovedEvent;
import alexiil.mc.lib.multipart.api.event.PartTickEvent;
import alexiil.mc.lib.multipart.api.property.MultipartProperties;
import alexiil.mc.lib.multipart.api.property.MultipartProperties.RedstonePowerProperty;
import alexiil.mc.lib.multipart.api.property.MultipartProperties.StrongRedstonePowerProperty;
import alexiil.mc.lib.multipart.api.property.MultipartProperty;
import alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer;
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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

public class PartContainer implements MultipartContainer {

    static final ParentNetIdSingle<PartContainer> NET_KEY;
    static final NetIdDataK<PartContainer> NET_ID_INITIAL_RENDER_DATA;
    static final NetIdDataK<PartContainer> NET_ID_ADD_PART;
    static final NetIdDataK<PartContainer> NET_ID_REMOVE_PART;
    static final NetIdDataK<PartContainer> NET_ID_REMOVE_PART_MULTI;
    static final NetIdSignalK<PartContainer> NET_SIGNAL_REDRAW;
    public static final ParentNetIdSingle<AbstractPart> NET_KEY_PART;

    static PartHolder extractPartHolder(AbstractPart value) {
        MultipartHolder holder = value.holder;
        if (holder == null || holder instanceof PartHolder) {
            return (PartHolder) holder;
        }
        throw new IllegalStateException(
            "Found an AbstractPart that doesn't have the correct class for it's holder! It should be using "
                + PartHolder.class.getName() + ", but instead it's using " + holder.getClass().getName() + "!"
        );
    }

    static {
        NET_KEY = MultipartBlockEntity.NET_KEY.extractor(
            PartContainer.class, "container", c -> c.blockEntity, b -> b.container
        );
        NET_ID_ADD_PART = NET_KEY.idData("add_part").setReceiver(PartContainer::readAddPart);
        NET_ID_REMOVE_PART = NET_KEY.idData("remove_part").setReceiver(PartContainer::readRemovePart);
        NET_ID_REMOVE_PART_MULTI = NET_KEY.idData("remove_part_multi").setReceiver(PartContainer::readRemovePartMulti);
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
    public final SimplePropertyContainer properties = new SimplePropertyContainer(this);

    public final List<PartHolder> parts = new ArrayList<>();
    final Long2ObjectMap<PartHolder> partsByUid = new Long2ObjectOpenHashMap<>();

    MultipartBlockEntity blockEntity;
    VoxelShape cachedShape = null;
    VoxelShape cachedCollisionShape = null;
    boolean havePropertiesChanged = false;
    boolean hasTicked = false;

    /** The next {@link PartHolder#uniqueId} to use. Incremented by one after every added part. */
    long nextId = 1;

    ImmutableList<PartModelKey> partModelKeys = ImmutableList.of();

    public PartContainer(MultipartBlockEntity blockEntity) {
        assert blockEntity != null : "The given blockEntity was null!";
        this.blockEntity = blockEntity;
    }

    // MultipartContainer

    @Override
    public World getMultipartWorld() {
        return blockEntity.getWorld();
    }

    @Override
    public BlockPos getMultipartPos() {
        return blockEntity.getPos();
    }

    @Override
    public BlockEntity getMultipartBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean canPlayerInteract(PlayerEntity player) {
        return blockEntity.canPlayerInteract(player);
    }

    @Override
    public BlockEntity getNeighbourBlockEntity(Direction dir) {
        return getMultipartWorld().getBlockEntity(getMultipartPos().offset(dir));
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
    public AbstractPart getPart(long uniqueId) {
        PartHolder holder = partsByUid.get(uniqueId);
        assert (holder == null) == (getAllParts(p -> p.holder.getUniqueId() == uniqueId).isEmpty());
        return holder == null ? null : holder.part;
    }

    @Override
    public PartOffer offerNewPart(MultipartCreator creator, boolean respectEntityBBs) {
        PartHolder holder = new PartHolder(this, creator);
        if (!canAdd(holder, true)) {
            return null;
        }
        return new PartOffer() {
            @Override
            public MultipartHolder getHolder() {
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
    public MultipartHolder addNewPart(MultipartCreator creator) {
        PartHolder holder = new PartHolder(this, creator);
        if (!canAdd(holder, true)) {
            return null;
        }
        addPartInternal(holder);
        return holder;
    }

    boolean canAdd(PartHolder offered, boolean respectEntityBBs) {
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

        if(respectEntityBBs) {
            VoxelShape collisionShape = offered.getPart().getCollisionShape();
            BlockPos pos = getMultipartPos();
            if ((!collisionShape.isEmpty()) && !getMultipartWorld().intersectsEntities(null, collisionShape.offset(pos.getX(), pos.getY(), pos.getZ()))) {
                return false;
            }
        }

        PartOfferedEvent event = new PartOfferedEvent(offered.part);
        eventBus.fireEvent(event);
        return event.isAllowed();
    }

    void addPartInternal(PartHolder holder) {
        assert holder.uniqueId == MultipartHolder.NOT_ADDED_UNIQUE_ID;
        holder.uniqueId = nextId++;
        parts.add(holder);
        partsByUid.put(holder.uniqueId, holder);
        holder.part.onAdded(eventBus);
        // Send the new part information *now* because other parts have a chance to send network packets
        sendNetworkUpdate(PartContainer.this, NET_ID_ADD_PART, (p, buffer, ctx) -> {
            holder.writeCreation(buffer, ctx);
        });
        recalculateShape();
        eventBus.fireEvent(new PartAddedEvent(holder.part));
        blockEntity.world().updateNeighbors(getMultipartPos(), blockEntity.getCachedState().getBlock());
    }

    private void readAddPart(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();
        PartHolder holder = new PartHolder(this, buffer, ctx);
        assert holder.part != null;
        parts.add(holder);
        partsByUid.put(holder.uniqueId, holder);
        holder.part.onAdded(eventBus);
        eventBus.fireEvent(new PartAddedEvent(holder.part));
        recalculateShape();
        redrawIfChanged();
    }

    @Override
    public boolean removePart(AbstractPart part) {
        PartHolder holder = (PartHolder) part.holder;
        int index = parts.indexOf(holder);
        if (index < 0) {
            return false;
        }
        if (!blockEntity.isServerWorld()) {
            return true;
        }

        if (holder.inverseRequiredParts == null) {
            removeSingle(index);
            return true;
        }

        // Full removal
        Set<PartHolder> toRemove = getAllRemoved(holder);

        Map<PartContainer, IntList> indexLists = new IdentityHashMap<>();
        for (PartHolder p : toRemove) {
            indexLists.computeIfAbsent(p.container, k -> new IntArrayList()).add(p.container.parts.indexOf(p));
        }

        for (Entry<PartContainer, IntList> entry : indexLists.entrySet()) {
            PartContainer c = entry.getKey();
            int[] indices = entry.getValue().toIntArray();
            if (indices.length == 1) {
                c.removeSingle(indices[0]);
            } else {
                c.removeMultiple(indices);
            }
        }
        return true;
    }

    /** @return Every {@link PartHolder} that will be removed if the given part holder was removed. */
    public static Set<PartHolder> getAllRemoved(PartHolder holder) {
        Set<PartHolder> toRemove = new ObjectOpenCustomHashSet<>(SystemUtil.identityHashStrategy());
        Set<PartHolder> openSet = new ObjectOpenCustomHashSet<>(SystemUtil.identityHashStrategy());
        openSet.add(holder);

        int iterationCount = 0;
        final int maxIterationCount = 10_000;
        while (!openSet.isEmpty()) {
            Iterator<PartHolder> iter = openSet.iterator();
            PartHolder next = iter.next();
            iter.remove();
            if (!toRemove.add(next)) {
                continue;
            }
            if (iterationCount++ > maxIterationCount) {
                LibMultiPart.LOGGER.warn(
                    "Tried to remove " + iterationCount + " parts, which seems a little excessive!"
                );
                break;
            }
            if (next.inverseRequiredParts == null) {
                continue;
            }
            openSet.addAll(next.inverseRequiredParts);
        }
        return toRemove;
    }

    private void removeSingle(int index) {
        PartHolder removed = parts.remove(index);
        assert removed != null;
        partsByUid.remove(removed.uniqueId);
        removed.clearRequiredParts();
        if (!parts.isEmpty()) {
            sendNetworkUpdate(this, NET_ID_REMOVE_PART, (p, buffer, ctx) -> {
                buffer.writeByte(index);
            });
        }

        // Now inform everything else that it was removed
        eventBus.removeListeners(removed.part);
        removed.part.onRemoved();
        properties.clearValues(removed.part);
        eventBus.fireEvent(new PartRemovedEvent(removed.part));

        postRemovePart();
    }

    private void postRemovePart() {
        if (parts.isEmpty()) {
            blockEntity.world().clearBlockState(getMultipartPos(), false);
        } else {
            recalculateShape();
            blockEntity.world().updateNeighbors(getMultipartPos(), blockEntity.getCachedState().getBlock());
        }
    }

    private void readRemovePart(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();

        int index = buffer.readUnsignedByte();
        if (index >= parts.size()) {
            throw new InvalidInputDataException("Invalid pluggable index " + index + " - we've probably got desynced!");
        }

        PartHolder removed = parts.remove(index);
        partsByUid.remove(removed.uniqueId);
        eventBus.removeListeners(removed.part);
        removed.part.onRemoved();
        properties.clearValues(removed.part);
        eventBus.fireEvent(new PartRemovedEvent(removed.part));
        recalculateShape();
        redrawIfChanged();
    }

    private void removeMultiple(int[] indices) {
        Arrays.sort(indices);
        ArrayUtil.reverse(indices);

        PartHolder[] holders = new PartHolder[indices.length];

        for (int i = 0; i < indices.length; i++) {
            int partIndex = indices[i];
            PartHolder holder = holders[i] = parts.remove(partIndex);
            holder.clearRequiredParts();
            partsByUid.remove(holder.uniqueId);
        }
        if (!isClientWorld()) {
            sendNetworkUpdate(this, NET_ID_REMOVE_PART_MULTI, (p, buffer, ctx) -> {
                buffer.writeByte(indices.length);
                for (int i : indices) {
                    buffer.writeByte(i);
                }
            });
        }

        for (PartHolder holder : holders) {
            eventBus.removeListeners(holder.part);
        }

        for (PartHolder holder : holders) {
            holder.part.onRemoved();
        }

        for (PartHolder holder : holders) {
            properties.clearValues(holder.part);
        }

        for (PartHolder holder : holders) {
            fireEvent(new PartRemovedEvent(holder.part));
        }

        if (isClientWorld()) {
            recalculateShape();
            redrawIfChanged();
        } else {
            postRemovePart();
        }
    }

    private void readRemovePartMulti(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();
        int count = buffer.readUnsignedByte();
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            indices[i] = buffer.readUnsignedByte();
        }
        removeMultiple(indices);
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
            if (cachedCollisionShape.isEmpty()) {
                cachedCollisionShape = MultipartBlock.MISSING_PARTS_SHAPE;
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
            // Just to make the world always re-render even though our state hasn't changed
            blockEntity.world().scheduleBlockRender(
                blockEntity.getPos(), Blocks.AIR.getDefaultState(), Blocks.VINE.getDefaultState()
            );
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
            partsByUid.put(holder.uniqueId, holder);
        }
        for (PartHolder holder : parts) {
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
    public MultipartEventBus getEventBus() {
        return eventBus;
    }

    @Override
    public boolean hasTicked() {
        return hasTicked;
    }

    @Override
    public MultipartPropertyContainer getProperties() {
        return properties;
    }

    // Internals

    void fromTag(CompoundTag tag) {
        if (LibMultiPart.DEBUG) {
            LibMultiPart.LOGGER.info("PartContainer.fromTag( " + getMultipartPos() + " ) {");
        }
        nextId = Long.MIN_VALUE;
        boolean areIdsValid = true;
        ListTag allPartsTag = tag.getList("parts", new CompoundTag().getType());
        for (int i = 0; i < allPartsTag.size(); i++) {
            CompoundTag partTag = allPartsTag.getCompoundTag(i);
            PartHolder holder = new PartHolder(this, partTag);
            if (holder.part != null) {
                parts.add(holder);
                if (!areIdsValid) {
                    continue;
                }
                nextId = Math.max(nextId, holder.uniqueId);
                PartHolder prev = partsByUid.put(holder.uniqueId, holder);
                if (prev != null) {
                    // Two parts with the same UID
                    areIdsValid = false;
                    partsByUid.clear();
                }
            }
        }

        if (parts.isEmpty()) {
            nextId = 0;
            if (LibMultiPart.DEBUG) {
                LibMultiPart.LOGGER.info("  parts is empty => nextId ← 0");
            }
        } else if (areIdsValid) {
            nextId++;
            if (LibMultiPart.DEBUG) {
                LibMultiPart.LOGGER.info("  parts are valid => nextId++ (nextId = " + nextId + ")");
            }
        } else {
            // One part had duplicate ID's.
            // This essentially means we read invalid data
            // so instead of dropping one of them we'll
            // reset the nextId to a random number and re-assign everything
            // (However always start way above 0 to make it less
            // likely to overlap with the previous - supposedly valid - values)
            nextId = ((long) new Random().nextInt() & 0x7fff_ffff) << 6l;

            if (LibMultiPart.DEBUG) {
                LibMultiPart.LOGGER.info("  parts are NOT valid => nextId ← rand (nextId = " + nextId + ")");
            }

            for (PartHolder holder : parts) {
                holder.uniqueId = nextId++;
                partsByUid.put(holder.uniqueId, holder);
            }
        }

        for (PartHolder holder : parts) {
            if (holder.unloadedRequiredParts != null) {
                Iterator<PosPartId> iterator = holder.unloadedRequiredParts.iterator();
                while (iterator.hasNext()) {
                    PosPartId id = iterator.next();
                    if (id.posEquals(blockEntity.getPos())) {
                        PartHolder other = partsByUid.get(id.uid);
                        if (other == null) {
                            // TODO: How can we handle this?
                            LibMultiPart.LOGGER.warn("Failed to resolve a part to part requirement! " + id);
                        } else {
                            holder.addRequiredPart0(other);
                        }
                        iterator.remove();
                    }
                }
            }
            // We don't need to go through unloadedInverseRequiredParts
            // because they must have been linked above
        }

        if (LibMultiPart.DEBUG) {
            LibMultiPart.LOGGER.info("}");
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
        delinkOtherBlockRequired();
    }

    void onChunkUnload() {
        eventBus.fireEvent(PartContainerState.CHUNK_UNLOAD);
        delinkOtherBlockRequired();
    }

    private void delinkOtherBlockRequired() {
        for (PartHolder holder : parts) {
            if (holder.requiredParts != null) {
                Iterator<PartHolder> iterator = holder.requiredParts.iterator();
                while (iterator.hasNext()) {
                    PartHolder req = iterator.next();
                    if (req.getContainer() == this) {
                        continue;
                    }
                    if (holder.unloadedRequiredParts == null) {
                        holder.unloadedRequiredParts = PartHolder.identityHashSet();
                    }
                    holder.unloadedRequiredParts.add(new PosPartId(req));
                    iterator.remove();

                    assert req.inverseRequiredParts != null;

                    if (req.unloadedInverseRequiredParts == null) {
                        req.unloadedInverseRequiredParts = PartHolder.identityHashSet();
                    }
                    req.unloadedInverseRequiredParts.add(new PosPartId(holder));
                    boolean didRemove = req.inverseRequiredParts.remove(holder);
                    assert didRemove;
                }
            }
            if (holder.inverseRequiredParts != null) {
                Iterator<PartHolder> iterator = holder.inverseRequiredParts.iterator();
                while (iterator.hasNext()) {
                    PartHolder req = iterator.next();
                    if (req.getContainer() == this) {
                        continue;
                    }
                    if (holder.unloadedInverseRequiredParts == null) {
                        holder.unloadedInverseRequiredParts = PartHolder.identityHashSet();
                    }
                    holder.unloadedInverseRequiredParts.add(new PosPartId(req));
                    iterator.remove();

                    assert req.requiredParts != null;

                    if (req.unloadedRequiredParts == null) {
                        req.unloadedRequiredParts = PartHolder.identityHashSet();
                    }
                    req.unloadedRequiredParts.add(new PosPartId(holder));
                    boolean didRemove = req.requiredParts.remove(holder);
                    assert didRemove;
                }
            }
        }
    }

    private void linkOtherBlockRequired() {
        if (LibMultiPart.DEBUG) {
            LibMultiPart.LOGGER.info("PartContainer.link_req( " + getMultipartPos() + " ) {");
        }
        Map<BlockPos, PartContainer> others = new HashMap<>();
        World world = getMultipartWorld();
        for (PartHolder holder : parts) {

            if (LibMultiPart.DEBUG) {
                LibMultiPart.LOGGER.info(" Holder " + holder.uniqueId + " " + holder.part.getClass());
            }

            if (holder.unloadedRequiredParts != null) {
                Iterator<PosPartId> iterator = holder.unloadedRequiredParts.iterator();
                while (iterator.hasNext()) {
                    PosPartId req = iterator.next();
                    if (LibMultiPart.DEBUG) {
                        LibMultiPart.LOGGER.info("  Required " + req);
                    }
                    if (!world.isBlockLoaded(req.pos)) {
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.info("    -- not loaded.");
                        }
                        continue;
                    }
                    iterator.remove();
                    PartContainer other = others.computeIfAbsent(req.pos, pos -> MultipartUtilImpl.get(world, pos));
                    if (other == null) {
                        // TODO: Log an error
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.warn("    -- not a multipart container");
                        }
                        continue;
                    }
                    AbstractPart otherPart = other.getPart(req.uid);
                    if (otherPart == null) {
                        // TODO: Log an error
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.warn("    -- didn't find uid!");
                        } else {
                            LibMultiPart.LOGGER.warn("[PartContainer.linkOtherBlockRequired] Failed to find the required part " + req.uid + " in " + other.parts + "!");
                        }
                        continue;
                    }
                    holder.addRequiredPart(otherPart);
                }
            }
            if (holder.unloadedInverseRequiredParts != null) {
                Iterator<PosPartId> iterator = holder.unloadedInverseRequiredParts.iterator();
                while (iterator.hasNext()) {
                    PosPartId invreq = iterator.next();
                    if (LibMultiPart.DEBUG) {
                        LibMultiPart.LOGGER.info("  InvReq " + invreq);
                    }
                    if (!world.isBlockLoaded(invreq.pos)) {
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.info("    -- not loaded.");
                        }
                        continue;
                    }
                    iterator.remove();
                    PartContainer other = others.computeIfAbsent(invreq.pos, pos -> MultipartUtilImpl.get(world, pos));
                    if (other == null) {
                        // TODO: Log an error
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.warn("    -- not a multipart container");
                        }
                        continue;
                    }
                    AbstractPart otherPart = other.getPart(invreq.uid);
                    if (otherPart == null) {
                        // TODO: Log an error
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.warn("    -- didn't find uid!");
                        } else {
                            LibMultiPart.LOGGER.warn("[PartContainer.linkOtherBlockRequired] Failed to find the required part " + invreq.uid + " in " + other.parts + "!");
                        }
                        continue;
                    }
                    otherPart.holder.addRequiredPart(holder.part);
                }
            }
            // Also: should removing a part force-load other blocks?
            // that seems like a reasonable idea...
        }

        if (LibMultiPart.DEBUG) {
            LibMultiPart.LOGGER.info("}");
        }
    }

    void onRemoved() {
        eventBus.fireEvent(PartContainerState.REMOVE);
        // for (PartHolder holder : parts) {
        // for (PartHolder inv : holder.inverseRequiredParts) {
        // if (inv.container == this) {
        // continue;
        // }
        // inv.remove();
        // }
        // }
    }

    void tick() {
        eventBus.fireEvent(PartTickEvent.INSTANCE);
        if (havePropertiesChanged) {
            havePropertiesChanged = false;
            final BlockState oldState = getMultipartWorld().getBlockState(getMultipartPos());
            BlockState state = oldState.with(
                MultipartBlock.EMITS_REDSTONE, properties.getValue(MultipartProperties.CAN_EMIT_REDSTONE)
            );
            state = state.with(MultipartBlock.LUMINANCE, properties.getValue(MultipartProperties.LIGHT_VALUE));
            boolean allowWater = properties.getValue(MultipartProperties.CAN_BE_WATERLOGGED);
            if (!allowWater && oldState.get(Properties.WATERLOGGED)) {
                state = state.with(Properties.WATERLOGGED, false);
            }
            if (state != oldState) {
                getMultipartWorld().setBlockState(getMultipartPos(), state);
            } else {
                getMultipartWorld().updateNeighbors(getMultipartPos(), LibMultiPart.BLOCK);
            }
        }

        if (!hasTicked) {
            hasTicked = true;
            linkOtherBlockRequired();
        }
    }

    void onListenerAdded(SingleListener<?> single) {
        // This was removed because minecraft doesn't seem to like removing a block entity while it's being ticked.

        // if (single.clazz == PartTickEvent.class && !(blockEntity instanceof Tickable)) {
        // World world = blockEntity.world();
        // if (world.getBlockEntity(getMultipartPos()) == blockEntity) {
        // world.setBlockEntity(getMultipartPos(), new MultipartBlockEntity.Ticking(this));
        // LibMultiPart.LOGGER.info("Switching " + getMultiPartPos() + " from non-ticking to ticking.");
        // } else {
        // LibMultiPart.LOGGER.info("Failed to switch " + getMultiPartPos() + " from non-ticking to ticking!");
        // }
        // }
    }

    void onListenerRemoved(SingleListener<?> single) {
        // Nothing needs to happen quite yet
    }

    <T> void onPropertyChanged(MultipartProperty<T> property, T old, T current) {

        if (!hasTicked) {
            // This can happen when loading
            havePropertiesChanged = true;
            return;
        }

        if (property == MultipartProperties.CAN_EMIT_REDSTONE) {
            BlockState state = getMultipartWorld().getBlockState(getMultipartPos());
            state = state.with(MultipartBlock.EMITS_REDSTONE, (Boolean) current);
            getMultipartWorld().setBlockState(getMultipartPos(), state);
            return;
        }

        if (property == MultipartProperties.LIGHT_VALUE) {
            BlockState state = getMultipartWorld().getBlockState(getMultipartPos());
            state = state.with(MultipartBlock.LUMINANCE, (Integer) current);
            getMultipartWorld().setBlockState(getMultipartPos(), state);
            return;
        }

        if (property == MultipartProperties.CAN_BE_WATERLOGGED) {
            BlockState state = getMultipartWorld().getBlockState(getMultipartPos());
            boolean val = Boolean.TRUE.equals(current);
            boolean stateV = state.get(Properties.WATERLOGGED);
            if (val || !stateV) {
                return;
            }
            state = state.with(Properties.WATERLOGGED, Boolean.FALSE);
            getMultipartWorld().setBlockState(getMultipartPos(), state);
            return;
        }

        if (property instanceof RedstonePowerProperty) {
            getMultipartWorld().updateNeighbors(getMultipartPos(), LibMultiPart.BLOCK);

            if (property instanceof StrongRedstonePowerProperty) {
                for (Direction dir : Direction.values()) {
                    getMultipartWorld().updateNeighbors(getMultipartPos().offset(dir), LibMultiPart.BLOCK);
                }
            }
            return;
        }
    }

    void addAllAttributes(AttributeList<?> list) {
        list.offer(this);
        for (PartHolder holder : parts) {
            holder.part.addAllAttributes(list);
        }
    }
}
