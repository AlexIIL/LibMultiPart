/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;

import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.NetIdTyped;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.Attributes;

import alexiil.mc.lib.multipart.api.event.MultipartEvent;
import alexiil.mc.lib.multipart.api.event.PartTickEvent;
import alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer;

public interface MultipartContainer {

    public static final Attribute<MultipartContainer> ATTRIBUTE = Attributes.create(MultipartContainer.class);

    // Outside object interaction

    World getMultipartWorld();

    BlockPos getMultipartPos();

    BlockEntity getMultipartBlockEntity();

    /** Calls {@link WorldChunk#setNeedsSaving(boolean)}. */
    void markChunkDirty();

    default DimensionType getDimension() {
        return getMultipartWorld().getDimension();
    }

    default boolean isClientWorld() {
        return getMultipartWorld().isClient;
    }

    /** @return true if the player should be able to interact with this container in GUI form. Implementors should
     *         generally check to ensure they are still present in-world. */
    boolean canPlayerInteract(PlayerEntity player);

    @Nullable
    BlockEntity getNeighbourBlockEntity(Direction dir);

    // Part Interaction

    List<AbstractPart> getAllParts();

    default List<AbstractPart> getAllParts(Predicate<AbstractPart> filter) {
        List<AbstractPart> list = new ArrayList<>();
        for (AbstractPart part : getAllParts()) {
            if (filter.test(part)) {
                list.add(part);
            }
        }
        return list;
    }

    default <P> List<P> getParts(Class<P> clazz) {
        List<P> list = new ArrayList<>();
        for (AbstractPart part : getAllParts()) {
            if (clazz.isInstance(part)) {
                list.add(clazz.cast(part));
            }
        }
        return list;
    }

    default <P> List<P> getParts(Class<P> clazz, Predicate<P> filter) {
        List<P> list = new ArrayList<>();
        for (AbstractPart part : getAllParts()) {
            if (clazz.isInstance(part)) {
                P typed = clazz.cast(part);
                if (filter.test(typed)) {
                    list.add(typed);
                }
            }
        }
        return list;
    }

    @Nullable
    default AbstractPart getFirstPart(Predicate<AbstractPart> filter) {
        List<AbstractPart> parts = getAllParts(filter);
        return parts.isEmpty() ? null : parts.get(0);
    }

    @Nullable
    default <P> P getFirstPart(Class<P> clazz) {
        List<P> parts = getParts(clazz);
        return parts.isEmpty() ? null : parts.get(0);
    }

    @Nullable
    default <P> P getFirstPart(Class<P> clazz, Predicate<P> filter) {
        List<P> parts = getParts(clazz, filter);
        return parts.isEmpty() ? null : parts.get(0);
    }

    /** Retrieves the part whose {@link AbstractPart#getOutlineShape()} contains the given {@link Vec3d}.
     * 
     * @param vec The vector. This should be in the range 0 to 1, rather than be local to this block.
     * @return The part, or null if there is no part at that vector. */
    @Nullable
    AbstractPart getPart(Vec3d vec);

    /** @return The part that has the given {@link MultipartHolder#getUniqueId() container-only unique ID}, or null if
     *         no parts have that unique id. */
    @Nullable
    AbstractPart getPart(long uniqueId);

    /** Offers a new part to this container. Note that this can be called on the client as well as the server, however
     * the client <em>cannot</em> add the resulting part offer to this container.
     * 
     * @param creator The creator which can create the actual part.
     * @param respectEntityBBs whether to respect nearby entities bounding boxes, or not
     * @return either null (if the offered part was refused) or an offer object which lets you either add it via
     *         {@link PartOffer#apply()}, or do nothing */
    @Nullable
    PartOffer offerNewPart(MultipartCreator creator, boolean respectEntityBBs);

    /** Offers a new part to this container, respecting nearby entities' bounding boxes. Note that this can be called on
     * the client as well as the server, however the client <em>cannot</em> add the resulting part offer to this
     * container.
     *
     * @param creator The creator which can create the actual part.
     * @return either null (if the offered part was refused) or an offer object which lets you either add it via
     *         {@link PartOffer#apply()}, or do nothing */
    @Nullable
    default PartOffer offerNewPart(MultipartCreator creator) {
        return offerNewPart(creator, true);
    }

    /** Shorter form of {@link #offerNewPart(MultipartCreator, boolean)} followed by adding the offer if it was allowed.
     * 
     * @return The holder for the part if it was added, or null if it was not. */
    @Nullable
    default MultipartHolder addNewPart(MultipartCreator creator, boolean respectEntityBBs) {
        PartOffer offer = offerNewPart(creator, respectEntityBBs);
        if (offer == null) {
            return null;
        }
        offer.apply();
        return offer.getHolder();
    }

    /** Shorter form of {@link #offerNewPart(MultipartCreator, boolean)} followed by adding the offer if it was allowed.
     *
     * @return The holder for the part if it was added, or null if it was not. */
    default MultipartHolder addNewPart(MultipartCreator creator) {
        return addNewPart(creator, true);
    }

    /** @return True if the part could have been added to the container, false otherwise.
     *         <p>
     *         Note that this will never actually add a part to the container, so it is safe to be called on the client
     *         side. */
    default boolean testNewPart(MultipartCreator creator, boolean respectEntityBBs) {
        return offerNewPart(creator, respectEntityBBs) != null;
    }

    /** @return True if the part could have been added to the container, false otherwise.
     *         <p>
     *         Note that this will never actually add a part to the container, so it is safe to be called on the client
     *         side. */
    default boolean testNewPart(MultipartCreator creator) {
        return offerNewPart(creator, true) != null;
    }

    @FunctionalInterface
    public interface MultipartCreator {
        AbstractPart create(MultipartHolder holder);
    }

    public interface PartOffer {
        MultipartHolder getHolder();

        /** Adds the part to the holder, throwing an exception if anything about the container changed in the time
         * between calling {@link MultipartContainer#offerNewPart(MultipartCreator, boolean)} and {@link #apply()}. */
        void apply();
    }

    /** @param part The part to remove
     * @return True if the part used to be contained by this container, false otherwise. */
    boolean removePart(AbstractPart part);

    // Shapes

    /** @return The current {@link VoxelShape} of every contained {@link AbstractPart#getShape()}. */
    VoxelShape getCurrentShape();

    /** @return The current {@link VoxelShape} of every contained {@link AbstractPart#getCollisionShape()}. */
    VoxelShape getCollisionShape();

    /** @return A complete {@link VoxelShape} of every contained {@link AbstractPart#getOutlineShape()} */
    VoxelShape getOutlineShape();

    /** Recalculates {@link #getCurrentShape()} and {@link #getCollisionShape()}. {@link AbstractPart}'s should call
     * this when their own shape changes. */
    void recalculateShape();

    // Networking

    /** Sends the given {@link NetIdDataK} or {@link NetIdSignalK} to every player currently watching this
     * {@link #getMultipartBlockEntity()}. */
    default <T> void sendNetworkUpdate(T obj, NetIdTyped<T> netId) {
        sendNetworkUpdateExcept(null, obj, netId);
    }

    /** Sends the given {@link NetIdDataK} to every player currently watching this {@link #getMultipartBlockEntity()},
     * with a custom {@link IMsgDataWriterK}. */
    default <T> void sendNetworkUpdate(T obj, NetIdDataK<T> netId, IMsgDataWriterK<T> writer) {
        sendNetworkUpdateExcept(null, obj, netId, writer);
    }

    /** Sends the given {@link NetIdDataK} or {@link NetIdSignalK} to every player currently watching this
     * {@link #getMultipartBlockEntity()}, except for the given player. */
    <T> void sendNetworkUpdateExcept(@Nullable PlayerEntity except, T obj, NetIdTyped<T> netId);

    /** Sends the given {@link NetIdDataK} to every player currently watching this {@link #getMultipartBlockEntity()},
     * with a custom {@link IMsgDataWriterK}, except for the given player. */
    <T> void sendNetworkUpdateExcept(
        @Nullable PlayerEntity except, T obj, NetIdDataK<T> netId, IMsgDataWriterK<T> writer
    );

    // Events

    MultipartEventBus getEventBus();

    /** Fires the given event on the {@link #getEventBus()} via {@link MultipartEventBus#fireEvent(MultipartEvent)}.
     * 
     * @return True if any listeners received the given event, false if none did. This may be useful for optimisation
     *         purposes.
     * @see MultipartEventBus#fireEvent(MultipartEvent) */
    default boolean fireEvent(MultipartEvent event) {
        return getEventBus().fireEvent(event);
    }

    /** @return True if {@link PartTickEvent} has been fired yet, or false if it hasn't. This is useful in cases where a
     *         part might need to do world-dependent calculations in {@link AbstractPart#onAdded(MultipartEventBus)} */
    boolean hasTicked();

    // Properties

    MultipartPropertyContainer getProperties();

    // Rendering

    /** Redraws this multipart, if any of it's parts return different {@link AbstractPart#getModelKey()}.
     * <p>
     * On the server this just sends a message to inform the client to check. */
    void redrawIfChanged();
}
