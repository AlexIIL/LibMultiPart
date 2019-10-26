/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.fabricmc.fabric.api.server.PlayerStream;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.multipart.api.event.NeighbourUpdateEvent;
import alexiil.mc.lib.multipart.mixin.api.IUnloadableBlockEntity;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdTyped;
import alexiil.mc.lib.net.ParentNetIdSingle;
import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.impl.McNetworkStack;

public class MultipartBlockEntity extends BlockEntity implements Tickable, IUnloadableBlockEntity {

    static final ParentNetIdSingle<MultipartBlockEntity> NET_KEY;

    static {
        NET_KEY = McNetworkStack.BLOCK_ENTITY.subType(MultipartBlockEntity.class, "libmultipart:container");
    }

    PartContainer container;

    public MultipartBlockEntity() {
        super(LibMultiPart.BLOCK_ENTITY);
        container = new PartContainer(this);
    }

    MultipartBlockEntity(PartContainer from) {
        super(LibMultiPart.BLOCK_ENTITY);
        this.container = from;
        container.blockEntity = this;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);
        if (tag.containsKey("container")) {
            container.fromTag(tag.getCompound("container"));
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        tag.put("container", container.toTag());
        return tag;
    }

    @Override
    public CompoundTag toInitialChunkDataTag() {
        sendNetworkUpdate(null, container, PartContainer.NET_ID_INITIAL_RENDER_DATA);
        return super.toInitialChunkDataTag();
    }

    @Nonnull
    World world() {
        final World w = world;
        if (w != null) {
            return w;
        } else {
            throw new IllegalStateException("This doesn't have a world!");
        }
    }

    @Override
    public void validate() {
        super.validate();
        container.validate();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        container.invalidate();
    }

    @Override
    public void onChunkUnload() {
        container.onChunkUnload();
    }

    public void onRemoved() {
        container.onRemoved();
    }

    public PartContainer getContainer() {
        return container;
    }

    public final boolean isServerWorld() {
        return world instanceof ServerWorld;
    }

    public final boolean isClientWorld() {
        return LibMultiPart.isWorldClientPredicate.test(world);
    }

    public boolean canPlayerInteract(PlayerEntity player) {
        if (world == null || world().getBlockEntity(pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 64.0D;
    }

    /** @return The {@link ActiveMinecraftConnection} to use to send to the specified player. */
    public final static ActiveMinecraftConnection getPlayerConnection(PlayerEntity player) {
        return CoreMinecraftNetUtil.getConnection(player);
    }

    /** @return The {@link ActiveMinecraftConnection} to use to send data to the server, from the client. */
    public final ActiveMinecraftConnection getClientConnection() {
        if (!world().isClient) {
            throw new IllegalArgumentException("We can't send data from the server to itself!");
        }
        return CoreMinecraftNetUtil.getClientConnection();
    }

    /** @return The {@link ActiveMinecraftConnection} to use to send to the specified player. */
    public final List<PlayerEntity> getPlayersWatching() {
        return PlayerStream.watching(this).collect(Collectors.toList());
    }

    /** Sends a network update update of the specified ID. */
    final <T> void sendNetworkUpdate(@Nullable PlayerEntity except, T obj, NetIdTyped<T> netId) {
        if (isClientWorld()) {
            netId.send(getClientConnection(), obj);
        } else if (isServerWorld()) {
            for (PlayerEntity player : getPlayersWatching()) {
                if (player != except) {
                    netId.send(getPlayerConnection(player), obj);
                }
            }
        }
    }

    /** Sends a network update update of the specified ID. */
    final <T> void sendNetworkUpdate(@Nullable PlayerEntity except, T obj, NetIdDataK<T> netId, IMsgDataWriterK<
        T> writer) {

        if (isClientWorld()) {
            netId.send(getClientConnection(), obj, writer);
        } else if (isServerWorld()) {
            for (PlayerEntity player : getPlayersWatching()) {
                if (player != except) {
                    netId.send(getPlayerConnection(player), obj, writer);
                }
            }
        }
    }

    // Events

    @Override
    public void tick() {
        container.tick();
    }

    void addAllAttributes(AttributeList<?> list) {
        container.addAllAttributes(list);
    }

    public void onNeighbourUpdate(BlockPos otherPos) {
        container.fireEvent(new NeighbourUpdateEvent(otherPos));
    }
}
