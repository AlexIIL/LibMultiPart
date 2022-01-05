/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.AbstractPart.ItemDropTarget;
import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.MultipartHolder;
import alexiil.mc.lib.multipart.api.PartDefinition;
import alexiil.mc.lib.multipart.api.PartLootParams;
import alexiil.mc.lib.multipart.api.PartLootParams.BrokenSinglePart;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

public final class PartHolder implements MultipartHolder {
    public final PartContainer container;
    public final AbstractPart part;

    long uniqueId = NOT_ADDED_UNIQUE_ID;

    /** Every {@link PartHolder} that this requires to be present in the {@link MultipartContainer}. */
    Set<PartHolder> requiredParts;

    /** Every {@link PartHolder} that requires this to be present in the {@link MultipartContainer}. This contains every
     * PartHolder that contains this in {@link #requiredParts}. */
    Set<PartHolder> inverseRequiredParts;

    /** Identical to {@link #requiredParts}, but instead contains the position and uid of every {@link PartHolder} that
     * is not currently loaded. */
    Set<PosPartId> unloadedRequiredParts;

    /** Identical to {@link #inverseRequiredParts}, but instead contains the position and uid of every
     * {@link PartHolder} that is not currently loaded. */
    Set<PosPartId> unloadedInverseRequiredParts;

    PartHolder(PartContainer container, MultipartCreator creator) {
        this.container = container;
        this.part = creator.create(this);
    }

    PartHolder(PartContainer container, NbtCompound tag) {
        this.container = container;
        String id = tag.getString("id");
        PartDefinition def = PartDefinition.PARTS.get(Identifier.tryParse(id));
        uniqueId = tag.getLong("uid");
        NbtCompound dataNbt = tag.getCompound("data");
        if (def == null) {
            part = new MissingPartImpl(this, id, dataNbt);
            LibMultiPart.LOGGER.warn(
                "Unknown part with ID '" + id + "': it has been turned into an unknown part at "
                    + container.getMultipartPos()
            );
        } else {
            part = def.readFromNbt(this, dataNbt);
            if (LibMultiPart.DEBUG) {
                LibMultiPart.LOGGER.info("  PartHolder.fromTag( " + uniqueId + ", " + part.getClass() + " ) {");
            }

            NbtElement reqltag = tag.get("req");
            if (reqltag instanceof NbtList) {
                NbtList reql = (NbtList) reqltag;
                for (int i = 0; i < reql.size(); i++) {
                    NbtCompound posPartTag = reql.getCompound(i);
                    if (LibMultiPart.DEBUG) {
                        LibMultiPart.LOGGER.info("    Required ( tag = " + posPartTag + " )");
                    }
                    if (!PosPartId.isValid(posPartTag)) {
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.info("      -- not valid!");
                        }
                        continue;
                    }
                    if (unloadedRequiredParts == null) {
                        unloadedRequiredParts = identityHashSet();
                    }
                    unloadedRequiredParts.add(new PosPartId(container, posPartTag));
                }
            }

            NbtElement invreqltag = tag.get("invReq");
            if (invreqltag instanceof NbtList) {
                NbtList invreql = (NbtList) invreqltag;
                for (int i = 0; i < invreql.size(); i++) {
                    NbtCompound posPartTag = invreql.getCompound(i);
                    if (LibMultiPart.DEBUG) {
                        LibMultiPart.LOGGER.info("    InvReq ( tag = " + posPartTag + " )");
                    }

                    if (!PosPartId.isValid(posPartTag)) {
                        if (LibMultiPart.DEBUG) {
                            LibMultiPart.LOGGER.info("      -- not valid!");
                        }
                        continue;
                    }
                    if (unloadedInverseRequiredParts == null) {
                        unloadedInverseRequiredParts = identityHashSet();
                    }
                    unloadedInverseRequiredParts.add(new PosPartId(container, posPartTag));
                }
            }
        }

        if (LibMultiPart.DEBUG) {
            LibMultiPart.LOGGER.info("  }");
        }
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("uid", uniqueId);
        if (part instanceof MissingPartImpl missing) {
            nbt.putString("id", missing.originalId);
            nbt.put("data", missing.originalNbt);
        } else {
            nbt.putString("id", part.definition.identifier.toString());
            nbt.put("data", part.toTag());
        }
        NbtList reql = new NbtList();
        if (requiredParts != null) {
            for (PartHolder req : requiredParts) {
                reql.add(new PosPartId(req).toTag(container));
            }
        }
        if (unloadedRequiredParts != null) {
            for (PosPartId req : unloadedRequiredParts) {
                reql.add(req.toTag(container));
            }
        }
        if (!reql.isEmpty()) {
            nbt.put("req", reql);
        }
        NbtList invReql = new NbtList();
        if (inverseRequiredParts != null) {
            for (PartHolder req : inverseRequiredParts) {
                invReql.add(new PosPartId(req).toTag(container));
            }
        }
        if (unloadedInverseRequiredParts != null) {
            for (PosPartId req : unloadedInverseRequiredParts) {
                invReql.add(req.toTag(container));
            }
        }
        if (!invReql.isEmpty()) {
            nbt.put("invReq", invReql);
        }
        return nbt;
    }

    PartHolder(PartContainer container, NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        uniqueId = buffer.readLong();
        this.container = container;
        Identifier identifier = buffer.readIdentifierSafe();
        PartDefinition def = PartDefinition.PARTS.get(identifier);
        if (def == null) {
            throw new InvalidInputDataException("Unknown remote part \"" + identifier + "\"");
        }
        part = def.loadFromBuffer(this, buffer, ctx);
    }

    void writeCreation(NetByteBuf buffer, IMsgWriteCtx ctx) {
        buffer.writeLong(uniqueId);
        buffer.writeIdentifier(part.definition.identifier);
        part.writeCreationData(buffer, ctx);
    }

    static <T> ObjectOpenCustomHashSet<T> identityHashSet() {
        return new ObjectOpenCustomHashSet<>(Util.identityHashStrategy());
    }

    @Override
    public String toString() {
        return "{PartHolder uid = " + uniqueId + ", part = " + part + "}";
    }

    @Override
    public MultipartContainer getContainer() {
        return container;
    }

    @Override
    public AbstractPart getPart() {
        return this.part;
    }

    @Override
    public void remove() {
        container.removePart(part);
    }

    @Override
    public void remove(PartRemoval... options) {
        if (container.isClientWorld()) {
            throw new IllegalStateException("Cannot remove a part on the client!");
        }

        int flags = 0;
        for (PartRemoval removal : options) {
            if ((flags & (1 << removal.ordinal())) == 0) {
                flags |= 1 << removal.ordinal();
                switch (removal) {
                    case DROP_ITEMS: {
                        dropItems(null);
                        break;
                    }
                    case BREAK_PARTICLES: {
                        part.sendNetworkUpdate(part, AbstractPart.NET_SPAWN_BREAK_PARTICLES);
                        break;
                    }
                    case BREAK_SOUND: {
                        callPlayBreakSound(part);
                        break;
                    }
                    default: {
                        throw new IllegalStateException("Unknown PartRemoval enum " + removal);
                    }
                }
            }
        }

        remove();
    }

    @Override
    public void dropItems(@Nullable PlayerEntity player) {
        LootContext context = createLootContext(player);
        part.addDrops(MultipartBlock.createDropTarget(part), context);
    }

    @Override
    public DefaultedList<ItemStack> collectDrops(PlayerEntity player) {
        LootContext context = createLootContext(player);
        DefaultedList<ItemStack> drops = DefaultedList.of();
        part.addDrops(new ItemDropCollector(drops), context);
        return drops;
    }

    private LootContext createLootContext(PlayerEntity player) {
        ServerWorld sv = (ServerWorld) container.getMultipartWorld();
        LootContext.Builder ctxBuilder = new LootContext.Builder(sv);
        ctxBuilder.random(sv.random);
        ctxBuilder.parameter(LootContextParameters.BLOCK_STATE, container.blockEntity.getCachedState());
        ctxBuilder.parameter(LootContextParameters.ORIGIN, Vec3d.ofCenter(container.getMultipartPos()));
        ctxBuilder.parameter(LootContextParameters.TOOL, ItemStack.EMPTY);
        ctxBuilder.optionalParameter(LootContextParameters.BLOCK_ENTITY, container.blockEntity);
        if (player != null) {
            ctxBuilder.optionalParameter(LootContextParameters.THIS_ENTITY, player);
        }
        ctxBuilder.parameter(PartLootParams.BROKEN_PART, new BrokenSinglePart(part));
        LootContext context = ctxBuilder.build(PartLootParams.PART_TYPE);
        return context;
    }

    // ###############################
    // # Play break sound reflection #
    // ###############################

    private static final BiFunction<AbstractPart, Object[], Void> CALL_PLAY_BREAK_SOUND
        = LmpReflection.getInstanceApiMethod(AbstractPart.class, "callPlayBreakSound", Void.class);

    private static void callPlayBreakSound(AbstractPart part) {
        CALL_PLAY_BREAK_SOUND.apply(part, null);
    }

    // ########################
    // # END play break sound #
    // ########################

    @Override
    public long getUniqueId() {
        return uniqueId;
    }

    @Override
    public boolean isPresent() {
        boolean hasUid = uniqueId != NOT_ADDED_UNIQUE_ID;
        assert container.parts.contains(this) == hasUid;
        return hasUid;
    }

    @Override
    public void addRequiredPart(AbstractPart other) {
        if (addRequiredPart0((PartHolder) other.holder)) {
            // Nothing to do ATM
        }
    }

    @Override
    public void removeRequiredPart(AbstractPart other) {
        if (removeRequiredPart0((PartHolder) other.holder)) {
            // Nothing to do ATM
        }
    }

    /** Internal method for adding a required part.
     * 
     * @return True if this changed anything, false otherwise. */
    boolean addRequiredPart0(PartHolder req) {
        if (req == null || req == this) {
            return false;
        }

        assert container.parts.contains(this);
        assert req.container.parts.contains(req);

        if (unloadedRequiredParts != null) {
            unloadedRequiredParts.remove(new PosPartId(req));
            if (unloadedRequiredParts.isEmpty()) {
                unloadedRequiredParts = null;
            }
        }
        if (requiredParts == null) {
            requiredParts = identityHashSet();
        }
        if (requiredParts.add(req)) {
            if (req.unloadedInverseRequiredParts != null) {
                Iterator<PosPartId> iterator = req.unloadedInverseRequiredParts.iterator();
                while (iterator.hasNext()) {
                    PosPartId id = iterator.next();
                    if (id.isFor(this)) {
                        iterator.remove();
                    }
                }
                if (req.unloadedInverseRequiredParts.isEmpty()) {
                    req.unloadedInverseRequiredParts = null;
                }
            }
            if (req.inverseRequiredParts == null) {
                req.inverseRequiredParts = identityHashSet();
            }
            boolean otherAdded = req.inverseRequiredParts.add(this);
            assert otherAdded : "We added but the other part didn't?";
            return true;
        }
        return false;
    }

    /** Internal method for removing a required part.
     * 
     * @return True if this changed anything, false otherwise. */
    boolean removeRequiredPart0(PartHolder req) {
        if (req == null || req == this) {
            return false;
        }
        if (unloadedRequiredParts != null) {
            unloadedRequiredParts.remove(new PosPartId(req));
            if (unloadedRequiredParts.isEmpty()) {
                unloadedRequiredParts = null;
            }
        }
        if (requiredParts == null) {
            return false;
        }
        if (requiredParts.remove(req)) {
            assert req.inverseRequiredParts != null;
            boolean didRemove = req.inverseRequiredParts.remove(this);
            assert didRemove : "We removed but the other part didn't?";
            if (req.inverseRequiredParts.isEmpty()) {
                req.inverseRequiredParts = null;
            }
            if (requiredParts.isEmpty()) {
                requiredParts = null;
            }
            return true;
        }
        return false;
    }

    public void clearRequiredParts() {
        unloadedRequiredParts = null;
        if (requiredParts == null) {
            return;
        }
        for (PartHolder holder : requiredParts.toArray(new PartHolder[0])) {
            removeRequiredPart(holder.part);
        }
        assert requiredParts == null : "Required Parts (" + requiredParts + ") wasn't fully cleared!";
    }

    void rotate(BlockRotation rotation) {
        part.rotate(rotation);
        unloadedRequiredParts = rotate(unloadedRequiredParts, rotation);
        unloadedInverseRequiredParts = rotate(unloadedInverseRequiredParts, rotation);
    }

    private Set<PosPartId> rotate(Set<PosPartId> parts, BlockRotation rotation) {
        if (parts == null) {
            return null;
        }
        Set<PosPartId> to = identityHashSet();
        for (PosPartId pos : parts) {
            to.add(pos.rotate(container, rotation));
        }
        return to;
    }

    void mirror(BlockMirror mirror) {
        part.mirror(mirror);
        unloadedRequiredParts = mirror(unloadedRequiredParts, mirror);
        unloadedInverseRequiredParts = mirror(unloadedInverseRequiredParts, mirror);
    }

    private Set<PosPartId> mirror(Set<PosPartId> parts, BlockMirror mirror) {
        if (parts == null) {
            return null;
        }
        Set<PosPartId> to = identityHashSet();
        for (PosPartId pos : parts) {
            to.add(pos.mirror(container, mirror));
        }
        return to;
    }
}
