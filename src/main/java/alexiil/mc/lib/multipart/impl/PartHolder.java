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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.MultipartHolder;
import alexiil.mc.lib.multipart.api.PartDefinition;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;

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

    PartHolder(PartContainer container, CompoundTag tag) {
        this.container = container;
        String id = tag.getString("id");
        PartDefinition def = PartDefinition.PARTS.get(Identifier.tryParse(id));
        uniqueId = tag.getLong("uid");
        if (def == null) {
            // The container shouldn't add this part
            part = null;
            LibMultiPart.LOGGER.warn(
                "Unknown part with ID '" + id + "': it has been removed from " + container.getMultipartPos()
            );
        } else {
            part = def.readFromNbt(this, tag.getCompound("data"));
            LibMultiPart.LOGGER.info("  PartHolder.fromTag( " + uniqueId + ", " + part.getClass() + " ) {");

            Tag reqltag = tag.getTag("req");
            if (reqltag instanceof ListTag) {
                ListTag reql = (ListTag) reqltag;
                for (int i = 0; i < reql.size(); i++) {
                    CompoundTag posPartTag = reql.getCompoundTag(i);
                    LibMultiPart.LOGGER.info("    Required ( tag = " + posPartTag + " )");
                    if (!PosPartId.isValid(posPartTag)) {
                        LibMultiPart.LOGGER.info("      -- not valid!");
                        continue;
                    }
                    if (unloadedRequiredParts == null) {
                        unloadedRequiredParts = identityHashSet();
                    }
                    unloadedRequiredParts.add(new PosPartId(posPartTag));
                }
            }

            Tag invreqltag = tag.getTag("invReq");
            if (invreqltag instanceof ListTag) {
                ListTag invreql = (ListTag) invreqltag;
                for (int i = 0; i < invreql.size(); i++) {
                    CompoundTag posPartTag = invreql.getCompoundTag(i);
                    LibMultiPart.LOGGER.info("    InvReq ( tag = " + posPartTag + " )");
                    if (!PosPartId.isValid(posPartTag)) {
                        LibMultiPart.LOGGER.info("      -- not valid!");
                        continue;
                    }
                    if (unloadedInverseRequiredParts == null) {
                        unloadedInverseRequiredParts = identityHashSet();
                    }
                    unloadedInverseRequiredParts.add(new PosPartId(posPartTag));
                }
            }
        }

        LibMultiPart.LOGGER.info("  }");
    }

    CompoundTag toTag() {
        CompoundTag nbt = new CompoundTag();
        if (part != null) {
            nbt.putLong("uid", uniqueId);
            nbt.putString("id", part.definition.identifier.toString());
            nbt.put("data", part.toTag());
            ListTag reql = new ListTag();
            if (requiredParts != null) {
                for (PartHolder req : requiredParts) {
                    reql.add(new PosPartId(req).toTag());
                }
            }
            if (unloadedRequiredParts != null) {
                for (PosPartId req : unloadedRequiredParts) {
                    reql.add(req.toTag());
                }
            }
            if (!reql.isEmpty()) {
                nbt.put("req", reql);
            }
            ListTag invReql = new ListTag();
            if (inverseRequiredParts != null) {
                for (PartHolder req : inverseRequiredParts) {
                    invReql.add(new PosPartId(req).toTag());
                }
            }
            if (unloadedInverseRequiredParts != null) {
                for (PosPartId req : unloadedInverseRequiredParts) {
                    invReql.add(req.toTag());
                }
            }
            if (!invReql.isEmpty()) {
                nbt.put("invReq", invReql);
            }
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
        return new ObjectOpenCustomHashSet<>(SystemUtil.identityHashStrategy());
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
}
