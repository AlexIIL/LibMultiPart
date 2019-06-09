package alexiil.mc.lib.multipart.api;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.net.ActiveConnection;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.McNetworkStack;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetObjectCache;
import alexiil.mc.lib.net.NetObjectCache.IEntrySerialiser;

import it.unimi.dsi.fastutil.Hash;

/** Contains the definition for an {@link AbstractPart}. Used for saving and loading, and syncing server -> client. */
public final class PartDefinition {
    public static final Map<Identifier, PartDefinition> PARTS = new HashMap<>();
    public static final NetObjectCache<PartDefinition> ID_NET_CACHE;

    static {
        ID_NET_CACHE = new NetObjectCache<>(
            McNetworkStack.ROOT.child("libmultipart:part_definition_cache"), new Hash.Strategy<PartDefinition>() {
                @Override
                public int hashCode(PartDefinition o) {
                    return o.identifier.hashCode();
                }

                @Override
                public boolean equals(PartDefinition a, PartDefinition b) {
                    return a.identifier.equals(b.identifier);
                }
            }, new IEntrySerialiser<PartDefinition>() {
                @Override
                public void write(PartDefinition obj, ActiveConnection connection, NetByteBuf buffer) {
                    buffer.writeIdentifier(obj.identifier);
                }

                @Override
                public PartDefinition read(ActiveConnection connection, NetByteBuf buffer)
                    throws InvalidInputDataException {
                    return PARTS.get(buffer.readIdentifierSafe());
                }
            }
        );
    }

    public final Identifier identifier;

    public final IPartNetLoader loader;
    public final IPartNbtReader reader;

    public PartDefinition(Identifier identifier, IPartNbtReader reader, IPartNetLoader loader) {
        this.identifier = identifier;
        this.reader = reader;
        this.loader = loader;
    }

    public void register(Identifier... oldNames) {
        PARTS.put(identifier, this);
        for (Identifier oldName : oldNames) {
            PARTS.put(oldName, this);
        }
    }

    public AbstractPart readFromNbt(MultiPartHolder holder, CompoundTag nbt) {
        return reader.readFromNbt(this, holder, nbt);
    }

    public AbstractPart loadFromBuffer(MultiPartHolder holder, NetByteBuf buffer, IMsgReadCtx ctx)
        throws InvalidInputDataException {
        return loader.loadFromBuffer(this, holder, buffer, ctx);
    }

    @FunctionalInterface
    public interface IPartNbtReader {
        /** Reads the pipe pluggable from NBT. Unlike {@link IPartNetLoader} (which is allowed to fail and throw an
         * exception if the wrong data is given) this should make a best effort to read the pluggable from nbt, or fall
         * back to sensible defaults. */
        AbstractPart readFromNbt(PartDefinition definition, MultiPartHolder holder, CompoundTag nbt);
    }

    @FunctionalInterface
    public interface IPartNetLoader {
        AbstractPart loadFromBuffer(PartDefinition definition, MultiPartHolder holder, NetByteBuf buffer,
            IMsgReadCtx ctx) throws InvalidInputDataException;
    }
}
