/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.Set;

import net.minecraft.nbt.CompoundTag;
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

    /** Every {@link PartHolder} that this requires to be present in the {@link MultipartContainer}. */
    Set<PartHolder> requiredParts;

    /** Every {@link PartHolder} that requires this to be present in the {@link MultipartContainer}. This contains every
     * PartHolder that contains this in {@link #requiredParts}. */
    Set<PartHolder> inverseRequiredParts;

    public PartHolder(PartContainer container, MultipartCreator creator) {
        this.container = container;
        this.part = creator.create(this);
    }

    public PartHolder(PartContainer container, CompoundTag tag) {
        this.container = container;
        String id = tag.getString("id");
        PartDefinition def = PartDefinition.PARTS.get(Identifier.ofNullable(id));
        if (def == null) {
            // The container shouldn't add this part
            part = null;
            LibMultiPart.LOGGER.warn(
                "Unknown part with ID '" + id + "': it has been removed from " + container.getMultipartPos()
            );
        } else {
            part = def.readFromNbt(this, tag.getCompound("data"));
        }
    }

    public CompoundTag toTag() {
        CompoundTag nbt = new CompoundTag();
        if (part != null) {
            nbt.putString("id", part.definition.identifier.toString());
            nbt.put("data", part.toTag());
        }
        return nbt;
    }

    public PartHolder(PartContainer container, NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {

        this.container = container;
        Identifier identifier = buffer.readIdentifierSafe();
        PartDefinition def = PartDefinition.PARTS.get(identifier);
        if (def == null) {
            throw new InvalidInputDataException("Unknown remote part \"" + identifier + "\"");
        }
        part = def.loadFromBuffer(this, buffer, ctx);
    }

    public void writeCreation(NetByteBuf buffer, IMsgWriteCtx ctx) {
        buffer.writeIdentifier(part.definition.identifier);
        part.writeCreationData(buffer, ctx);
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
    public int getPartIndex() {
        return container.parts.indexOf(this);
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
    private boolean addRequiredPart0(PartHolder req) {
        if (req == null || req == this) {
            return false;
        }

        assert container == req.container;
        assert container.parts.contains(this);
        assert container.parts.contains(req);

        if (requiredParts == null) {
            requiredParts = new ObjectOpenCustomHashSet<>(SystemUtil.identityHashStrategy());
        }
        if (requiredParts.add(req)) {
            if (req.inverseRequiredParts == null) {
                req.inverseRequiredParts = new ObjectOpenCustomHashSet<>(SystemUtil.identityHashStrategy());
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
    private boolean removeRequiredPart0(PartHolder req) {
        if (req == null || req == this || requiredParts == null) {
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
        if (requiredParts == null) {
            return;
        }
        for (PartHolder holder : requiredParts.toArray(new PartHolder[0])) {
            removeRequiredPart(holder.part);
        }
        assert requiredParts == null : "Required Parts (" + requiredParts + ") wasn't fully cleared!";
    }
}
