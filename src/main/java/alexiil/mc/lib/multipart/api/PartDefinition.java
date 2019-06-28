/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.McNetworkStack;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetObjectCache;

/** Contains the definition for an {@link AbstractPart}. Used for saving and loading, and syncing server -> client. */
public final class PartDefinition {
    public static final Map<Identifier, PartDefinition> PARTS = new HashMap<>();
    public static final NetObjectCache<PartDefinition> ID_NET_CACHE;

    static {
        ID_NET_CACHE = NetObjectCache.createMappedIdentifier(
            McNetworkStack.ROOT.child("libmultipart:part_definition_cache"), def -> def.identifier, PARTS
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

    public AbstractPart readFromNbt(MultipartHolder holder, CompoundTag nbt) {
        return reader.readFromNbt(this, holder, nbt);
    }

    public AbstractPart loadFromBuffer(MultipartHolder holder, NetByteBuf buffer, IMsgReadCtx ctx)
        throws InvalidInputDataException {
        return loader.loadFromBuffer(this, holder, buffer, ctx);
    }

    @FunctionalInterface
    public interface IPartNbtReader {
        /** Reads the pipe pluggable from NBT. Unlike {@link IPartNetLoader} (which is allowed to fail and throw an
         * exception if the wrong data is given) this should make a best effort to read the pluggable from nbt, or fall
         * back to sensible defaults. */
        AbstractPart readFromNbt(PartDefinition definition, MultipartHolder holder, CompoundTag nbt);
    }

    @FunctionalInterface
    public interface IPartNetLoader {
        AbstractPart loadFromBuffer(PartDefinition definition, MultipartHolder holder, NetByteBuf buffer,
            IMsgReadCtx ctx) throws InvalidInputDataException;
    }
}
