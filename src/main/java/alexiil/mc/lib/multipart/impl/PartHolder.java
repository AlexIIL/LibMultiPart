package alexiil.mc.lib.multipart.impl;

import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultiPartContainer;
import alexiil.mc.lib.multipart.api.MultiPartHolder;
import alexiil.mc.lib.multipart.api.PartDefinition;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;

public final class PartHolder implements MultiPartHolder {

    public final PartContainer container;
    public final AbstractPart part;

    public PartHolder(PartContainer container, Function<MultiPartHolder, AbstractPart> creator) {
        this.container = container;
        this.part = creator.apply(this);
    }

    public PartHolder(PartContainer container, CompoundTag tag) {
        this.container = container;
        String id = tag.getString("id");
        PartDefinition def = PartDefinition.PARTS.get(Identifier.ofNullable(id));
        if (def == null) {
            // The container shouldn't add this part
            part = null;
            LibMultiPart.LOGGER.warn(
                "Unknown part with ID '" + id + "': it has been removed from " + container.getMultiPartPos()
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
    public MultiPartContainer getContainer() {
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
}
