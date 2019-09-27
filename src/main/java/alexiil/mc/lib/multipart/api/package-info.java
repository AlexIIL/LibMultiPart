/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
/**
 * <h1>LibMultiPart's API.</h1>
 * <p>
 * This package contains everything needed to interact with libmultipart - you should never need to use anything in the
 * {@link alexiil.mc.lib.multipart.impl} package.
 * <p>
 * There are a few main concepts:
 * <ol>
 * <li>Containers</li>
 * <li>Individual Parts</li>
 * <li>Part Shapes</li>
 * <li>Accessing Parts</li>
 * <li>Adding Parts</li>
 * <li>Part Requirements</li>
 * <li>Removing Parts</li>
 * <li>Client <-> Server Networking</li>
 * <li>Events</li>
 * <li>Properties</li>
 * <li>Rendering</li>
 * <li>Converting normal blocks into parts</li>
 * </ol>
 * <h2>Containers</h2>
 * <p>
 * Every multipart is contained a {@link net.minecraft.block.entity.BlockEntity BlockEntity}, and is exposed through the
 * {@link alexiil.mc.lib.multipart.api.MultipartContainer MultipartContainer} interface. This is exposed through a
 * LibBlockAttributes {@link alexiil.mc.lib.attributes.Attribute Attribute} in
 * {@link alexiil.mc.lib.multipart.api.MultipartContainer#ATTRIBUTE MultipartContainer.ATTRIBUTE}
 * <p>
 * <h2>Individual Parts</h2>
 * <p>
 * Every part must extend from the base class {@link alexiil.mc.lib.multipart.api.AbstractPart AbstractPart}, and be
 * identified by a {@link alexiil.mc.lib.multipart.api.PartDefinition PartDefinition}. These definitions must be
 * (manually) added to the part definition map {@link alexiil.mc.lib.multipart.api.PartDefinition#PARTS
 * PartDefinition.PARTS}.
 * <p>
 * The {@link alexiil.mc.lib.multipart.api.MultipartHolder MultipartHolder} interface acts as the bridge between the
 * container implementation and the part, and contains a lot more information to simplify reading and writing individual
 * parts.
 * <p>
 * Every part has a container-only unique ID ({@link alexiil.mc.lib.multipart.api.MultipartHolder#getUniqueId()
 * MultipartHolder.getUniqueId()}) which can be used to store a reference to an exact part at a given position.
 * <p>
 * <h2>Part Shapes</h2>
 * <p>
 * Every part has 3 shape methods that they can override: {@link alexiil.mc.lib.multipart.api.AbstractPart#getShape()
 * getShape()}, {@link alexiil.mc.lib.multipart.api.AbstractPart#getCollisionShape() getCollisionShape()}, and
 * {@link alexiil.mc.lib.multipart.api.AbstractPart#getDynamicShape(float) getDynamicShape()}. The first method
 * (getShape) is abstract, and is used for detecting if two parts occupy the same space inside a block. The second
 * method (getCollisionShape) is used for nearly everything else - calculating lighting, solidity logic, and collisions
 * with entities. If this isn't overridden then it defaults to returning the main getShape(). The final method
 * (getDynamicShape()) is used for ray-tracing and client-side bounding boxes. It has a partialTicks parameter, which
 * allows the bounding box to follow the motion of a part. (For example a buildcraft engine, or a buildcraft pipe
 * pulser).
 * <p>
 * <h2>Accessing Parts</h2>
 * <p>
 * Individual parts can be accessed via the various container methods all named similar to "getPart" or "getAllParts".
 * For example {@link alexiil.mc.lib.multipart.api.MultipartContainer#getPart(long) MultipartContainer.getPart(long)}
 * will get the part with the given unique ID.
 * <p>
 * <h2>Adding Parts</h2>
 * <p>
 * New parts can be added in two ways:
 * <ol>
 * <li>From nothing:
 * {@link alexiil.mc.lib.multipart.api.MultipartUtil#offerNewPart(net.minecraft.world.World, net.minecraft.util.math.BlockPos, alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator)
 * MultipartUtil.offerNewPart(World, BlockPos, MultipartContainer.MultipartCreator)}</li>
 * <li>To an existing MultipartContainer:
 * {@link alexiil.mc.lib.multipart.api.MultipartContainer#offerNewPart(alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator)
 * MultipartContainer.offerNewpart(MultipartContainer.MultipartCreator)}</li>
 * </ol>
 * Both of these methods have the same basic behaviour: they will not affect the world/container unless
 * {@link alexiil.mc.lib.multipart.api.MultipartContainer.PartOffer#apply() PartOffer.apply()} is called on the returned
 * offer.
 * <p>
 * <h2>Part Requirements</h2>
 * <p>
 * If one part "requires" another then it will always be broken if the required part is broken. This works across
 * multiple blocks. To make one part require another you should call
 * {@link alexiil.mc.lib.multipart.api.MultipartHolder#addRequiredPart(AbstractPart) MultipartHolder.addRequiredPart}.
 * <p>
 * <h2>Removing Parts</h2>
 * <p>
 * Removing a single part is simple: just call {@link alexiil.mc.lib.multipart.api.MultipartHolder#remove()
 * MultipartHolder.remove()} or {@link alexiil.mc.lib.multipart.api.MultipartContainer#removePart(AbstractPart)
 * MultipartContainer.removePart(AbstractPart)}. This will remove both the given part and every part that required it.
 * This will not drop any of the items.
 * <p>
 * <h2>Client <-> Server Networking</h2>
 * <p>
 * Multipart's use {@link alexiil.mc.lib.net LibNetworkStack} for all networking operations, exposed through
 * {@link alexiil.mc.lib.multipart.api.AbstractPart#NET_ID AbstractPart.NET_ID}.
 * <p>
 * <h2>Events</h2>
 * <p>
 * Unlike {@link net.minecraft.block.Block blocks} or {@link net.minecraft.entity.Entity entities} most events (like
 * neighbour updates, ticks, entity collision, etc) are delivered through
 * {@link alexiil.mc.lib.multipart.api.event.MultipartEvent MultipartEvent} objects through the
 * {@link alexiil.mc.lib.multipart.api.MultipartEventBus MultipartEventBus}. You can register listeners for these events
 * in {@link alexiil.mc.lib.multipart.api.AbstractPart#onAdded(MultipartEventBus)
 * AbstractPart.onAdded(MultipartEventBus)}.
 * <p>
 * There are a few core events:
 * <ul>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartOfferedEvent PartOfferedEvent}, which is fired whenever a part is
 * offered to the container.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartAddedEvent PartAddedEvent}, which is fired after the part is
 * offered and has been added to the container.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartContainerState.ChunkUnload PartContainerState.ChunkUnload}, which
 * is fired whenever the chunk containing the container is unloaded.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartContainerState.Invalidate PartContainerState.Invalidate}, which is
 * fired whenever the BlockEntity containing the parts is {@link net.minecraft.block.entity.BlockEntity#invalidate()
 * invalidated}.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartContainerState.Validate PartContainerState.Validate}, which is
 * fired whenever the BlockEntity containing the parts is {@link net.minecraft.block.entity.BlockEntity#validate()
 * validated}.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartContainerState.Remove PartContainerState.Remove}, which is fired
 * whenever the BlockEntity containing the parts is
 * {@link net.minecraft.block.Block#onBlockRemoved(net.minecraft.block.BlockState, net.minecraft.world.World, net.minecraft.util.math.BlockPos, net.minecraft.block.BlockState, boolean)
 * removed}.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartTickEvent PartTickEvent}, which is fired once when the BlockEntity
 * containing it is ticked.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartListenerAdded PartListenerAdded}, which is fired whenever a new
 * listener is added to the event bus. This is only exposed to allow parts to optimise-out event calls on a per-part
 * basis, and is not useful in normal scenarios.</li>
 * <li>{@link alexiil.mc.lib.multipart.api.event.PartListenerRemoved PartListenerRemoved}, which is fired whenever a
 * listener is removed from the event bus. This is only exposed to allow parts to optimise-out event calls on a per-part
 * basis, and is not useful in normal scenarios.</li>
 * </ul>
 * <p>
 * <h2>Properties</h2>
 * <p>
 * Properties are the way to change how the multipart block as a whole works - for example you can make the block emit
 * light, redstone, etc. Usage is simple: each multipart has a
 * {@link alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer MultipartPropertyContainer}, which can be
 * used to map a specific part to a value for any given property, via
 * {@link alexiil.mc.lib.multipart.api.property.MultipartPropertyContainer #setValue(Object, alexiil.mc.lib.multipart.api.property.MultipartProperty, Object)
 * setValue}. The default properties are static final fields in
 * {@link alexiil.mc.lib.multipart.api.property.MultipartProperties}. Changing the value of a property as a whole will
 * fire a {@link alexiil.mc.lib.multipart.api.property.PartPropertyChangedEvent PartPropertyChangedEvent}.
 * <p>
 * <h2>Rendering</h2>
 * <p>
 * There are two types of renderers: static and dynamic. Both renderers are registered in
 * {@link alexiil.mc.lib.multipart.api.render.MultipartRenderRegistry MultipartRenderRegistry}. Static renderers are
 * akin to normal block models, except you have to:
 * <ol>
 * <li>Return a custom {@link alexiil.mc.lib.multipart.api.render.PartModelKey PartModelKey} from
 * {@link alexiil.mc.lib.multipart.api.AbstractPart #getModelKey() AbstractPart#getModelKey()}</li>
 * <li>Register a handler for that <em>exact</em> class in
 * {@link alexiil.mc.lib.multipart.api.render.MultipartRenderRegistry# registerBaker(Class, alexiil.mc.lib.multipart.api.render.PartModelBaker)
 * MultipartRenderRegistry#registerBaker()}</li>
 * <li>Actually emit the quads in
 * {@link alexiil.mc.lib.multipart.api.render.PartModelBaker#emitQuads(alexiil.mc.lib.multipart.api.render.PartModelKey, alexiil.mc.lib.multipart.api.render.PartRenderContext)
 * PartModelBaker#emitQuads()}</li>
 * </ol>
 * <p>
 * Dynamic rendering is slightly simpler, as you do not have to go through a
 * {@link alexiil.mc.lib.multipart.api.render.PartModelKey model key object} in order to render your quads.
 * <ol>
 * <li>Register a handler for the <em>exact</em> {@link alexiil.mc.lib.multipart.api.AbstractPart AbstractPart} class
 * that you want to render in
 * {@link alexiil.mc.lib.multipart.api.render.MultipartRenderRegistry#registerRenderer(Class, alexiil.mc.lib.multipart.api.render.PartRenderer)
 * MultipartRenderRegistry#registerRenderer()}</li>
 * <li>Emit the quads in
 * {@link alexiil.mc.lib.multipart.api.render.PartRenderer#render(AbstractPart, double, double, double, float, int)
 * PartRenderer#render()}.</li>
 * </ol>
 * <p>
 * <h2>Converting normal blocks into parts</h2>
 * <p>
 * If you wish a custom {@link net.minecraft.block.Block Block} to be turned into a multipart block (when another
 * multipart is placed) you need to implement {@link alexiil.mc.lib.multipart.api.NativeMultipart NativeMultipart}, and
 * then return a non-null list.
 * <p>
 * You can initiate this change at any time by calling
 * {@link alexiil.mc.lib.multipart.api.MultipartUtil#turnIntoMultipart(net.minecraft.world.World, net.minecraft.util.math.BlockPos)
 * MultipartUtil#turnIntoMultipart()}. */
package alexiil.mc.lib.multipart.api;
