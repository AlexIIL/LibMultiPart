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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.multipart.api.event.MultipartEvent;
import alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.NetIdTyped;

public interface MultipartContainer {

    public static final Attribute<MultipartContainer> ATTRIBUTE = Attributes.create(MultipartContainer.class);

    // Outside object interaction

    World getMultipartWorld();

    BlockPos getMultipartPos();

    BlockEntity getMultipartBlockEntity();

    default DimensionType getDimension() {
        return getMultipartWorld().dimension.getType();
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
    PartOffer offerNewPart(MultipartCreator creator);

    @Nullable
    default MultipartHolder addNewPart(MultipartCreator creator) {
        PartOffer offer = offerNewPart(creator);
        if (offer == null) {
            return null;
        }
        offer.apply();
        return offer.getHolder();
    }

    /** @return True if the part could have been added to the container, false otherwise.
     *         <p>
     *         Note that this will never actually add a part to the container, so it is safe to be called on the client
     *         side. */
    default boolean testNewPart(MultipartCreator creator) {
        return offerNewPart(creator) != null;
    }

    @FunctionalInterface
    public interface MultipartCreator {
        AbstractPart create(MultipartHolder holder);
    }

    public interface PartOffer {
        MultipartHolder getHolder();

        /** Adds the part to the holder, throwing an exception if anything about the container changed in the time
         * between calling {@link MultipartContainer#offerNewPart(MultipartCreator)} and {@link #apply()}. */
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

    /** @return A complete {@link VoxelShape} of every contained {@link AbstractPart#getDynamicShape(float)} */
    VoxelShape getDynamicShape(float partialTicks);

    /** Recalculates {@link #getCurrentShape()} and {@link #getCollisionShape()}. {@link AbstractPart}'s should call
     * this when their own shape changes. */
    void recalculateShape();

    // Networking

    /** Sends the given {@link NetIdDataK} or {@link NetIdSignalK} to every player currently watching this
     * {@link #getMultipartBlockEntity()}. */
    <T> void sendNetworkUpdate(T obj, NetIdTyped<T> netId);

    /** Sends the given {@link NetIdDataK} to every player currently watching this {@link #getMultipartBlockEntity()},
     * with a custom {@link IMsgDataWriterK}. */
    <T> void sendNetworkUpdate(T obj, NetIdDataK<T> netId, IMsgDataWriterK<T> writer);

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

    // Properties

    MultipartPropertyContainer getProperties();

    // Rendering

    void redrawIfChanged();
}
