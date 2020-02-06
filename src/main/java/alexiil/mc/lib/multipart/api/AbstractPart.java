/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import javax.annotation.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.event.EventListener;
import alexiil.mc.lib.multipart.api.render.PartModelBaker;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import alexiil.mc.lib.multipart.impl.PartContainer;
import alexiil.mc.lib.multipart.impl.client.SingleSpriteProvider;
import alexiil.mc.lib.multipart.mixin.impl.BlockSoundGroupAccessor;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.NetIdTyped;
import alexiil.mc.lib.net.ParentNetIdSingle;

/** The base class for every part in a multipart block.
 * <p>
 * Generally implementations will want to override (in addition to the abstract methods):
 * <ul>
 * <li>{@link #getPickStack()} (and optionally {@link #addDrops(DefaultedList)}}</li>
 * </ul>
 */
public abstract class AbstractPart {

    public static final ParentNetIdSingle<AbstractPart> NET_ID;
    public static final NetIdDataK<AbstractPart> NET_RENDER_DATA;
    public static final NetIdSignalK<AbstractPart> NET_SPAWN_BREAK_PARTICLES;

    static {
        NET_ID = PartContainer.NET_KEY_PART;
        NET_RENDER_DATA = NET_ID.idData("render_data").toClientOnly()
            .setReadWrite(AbstractPart::readRenderData, AbstractPart::writeRenderData);
        // never buffer break particles because otherwise the block will be removed before the particles can spawn
        NET_SPAWN_BREAK_PARTICLES = NET_ID.idSignal("spawn_break_particles").toClientOnly().withoutBuffering()
            .setReceiver(AbstractPart::spawnBreakParticles);
    }

    public final PartDefinition definition;
    public final MultipartHolder holder;

    public AbstractPart(PartDefinition definition, MultipartHolder holder) {
        this.definition = definition;
        this.holder = holder;
    }

    public CompoundTag toTag() {
        return new CompoundTag();
    }

    /** Writes the payload that will be passed into
     * {@link PartDefinition#loadFromBuffer(MultipartHolder, NetByteBuf, IMsgReadCtx)} on the client. (This is called on
     * the server and sent to the client). Note that this will be called *instead* of write and read payload.
     * 
     * @param ctx TODO */
    public void writeCreationData(NetByteBuf buffer, IMsgWriteCtx ctx) {}

    public void writeRenderData(NetByteBuf buffer, IMsgWriteCtx ctx) {}

    public void readRenderData(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {}

    /** Sends the given {@link NetIdDataK} or {@link NetIdSignalK} to every player currently watching this multipart. */
    public final <T> void sendNetworkUpdate(T obj, NetIdTyped<T> netId) {
        holder.getContainer().sendNetworkUpdate(obj, netId);
    }

    /** Sends the given {@link NetIdDataK} to every player currently watching this multipart, with a custom
     * {@link IMsgDataWriterK}. */
    public final <T> void sendNetworkUpdate(T obj, NetIdDataK<T> netId, IMsgDataWriterK<T> writer) {
        holder.getContainer().sendNetworkUpdate(obj, netId, writer);
    }

    /** Sends the given {@link NetIdDataK} or {@link NetIdSignalK} to every player currently watching this multipart,
     * except for the given player. */
    public final <T> void sendNetworkUpdateExcept(@Nullable PlayerEntity except, T obj, NetIdTyped<T> netId) {
        holder.getContainer().sendNetworkUpdateExcept(except, obj, netId);
    }

    /** Sends the given {@link NetIdDataK} to every player currently watching this multipart, with a custom
     * {@link IMsgDataWriterK}, except for the given player. */
    public final <T> void sendNetworkUpdateExcept(
        @Nullable PlayerEntity except, T obj, NetIdDataK<T> netId, IMsgDataWriterK<T> writer
    ) {
        holder.getContainer().sendNetworkUpdateExcept(except, obj, netId, writer);
    }

    /** Called whenever this part was added to the {@link MultipartContainer}, either in
     * {@link BlockEntity#cancelRemoval()} or when it is manually added by an item.
     * <p>
     * Register event handlers (as methods) with {@link MultipartEventBus#addListener(Object, Class, EventListener)}.
     * 
     * @param bus The event bus to register with. This is shorthand for
     *            <code>holder.getContainer().getEventBus()</code> */
    public void onAdded(MultipartEventBus bus) {
        // Nothing registers by default.
    }

    public void onRemoved() {
        // Nothing to do by default
    }

    /** Open method, that's designed to be called from the {@link Item} that places this part into the world.
     * <p>
     * (Nothing calls this by default, as all parts are placed from a custom item). */
    public void onPlacedBy(PlayerEntity player, Hand hand) {
        // Nothing to do by default
    }

    /** Called instead of {@link Block#onBreak(World, BlockPos, BlockState, PlayerEntity)}, to play the broken sound,
     * and spawn break particles.
     * 
     * @return True if this should prevent {@link Block#onBreak(World, BlockPos, BlockState, PlayerEntity)} from being
     *         called afterwards, false otherwise. */
    public boolean onBreak(PlayerEntity player) {
        if (!holder.getContainer().isClientWorld()) {
            playBreakSound();
            sendNetworkUpdate(this, NET_SPAWN_BREAK_PARTICLES);
        }
        return true;
    }

    /** Called by default in {@link #onBreak(PlayerEntity)} to play the breaking sound. The default implementation calls
     * {@link #playBreakSound(BlockState)} with {@link Blocks#STONE}, however you are encouraged to call it with a more
     * appropriate sound or */
    protected void playBreakSound() {
        playBreakSound(Blocks.STONE.getDefaultState());
    }

    protected final void playBreakSound(BlockState blockState) {
        World world = holder.getContainer().getMultipartWorld();
        BlockSoundGroup group = blockState.getSoundGroup();
        world.playSound(
            null, holder.getContainer().getMultipartPos(),
            ((BlockSoundGroupAccessor) group).libmultipart_getBreakSound(), SoundCategory.BLOCKS,
            (group.getVolume() + 1.0F) / 2.0F, group.getPitch() * 0.8F
        );
    }

    private final void spawnBreakParticles(IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();
        spawnBreakParticles();
    }

    /** Spawns a single breaking particle.
     * 
     * @param side The side that was hit
     * @return True to cancel the default breaking particle from spawning, false otherwise. */
    @Environment(EnvType.CLIENT)
    public boolean spawnBreakingParticles(Direction side) {
        // TODO: Implement this!
        return false;
    }

    /** Called on the client to spawn break particles. This calls {@link #spawnBreakParticles(BlockState)} with
     * {@link Blocks#STONE} by default, however you are encouraged to override this. */
    @Environment(EnvType.CLIENT)
    protected void spawnBreakParticles() {
        spawnBreakParticles(Blocks.STONE.getDefaultState());
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnBreakParticles(BlockState state) {
        spawnBreakParticles(state, (Sprite) null);
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnBreakParticles(BlockState state, @Nullable Identifier spriteId) {
        Sprite sprite;
        if (spriteId == null) {
            sprite = null;
        } else {
            sprite = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEX).apply(spriteId);
        }
        spawnBreakParticles(state, sprite);
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnBreakParticles(BlockState state, @Nullable Sprite sprite) {
        World world = holder.getContainer().getMultipartWorld();
        BlockPos pos = holder.getContainer().getMultipartPos();
        ParticleManager manager = MinecraftClient.getInstance().particleManager;
        VoxelShape voxelShape = getOutlineShape();
        for (Box box : voxelShape.getBoundingBoxes()) {
            double x0 = box.x1;
            double y0 = box.y1;
            double z0 = box.z1;
            double x1 = box.x2;
            double y1 = box.y2;
            double z1 = box.z2;
            double minX = Math.min(1.0D, x1 - x0);
            double minY = Math.min(1.0D, y1 - y0);
            double minZ = Math.min(1.0D, z1 - z0);
            int maxX = Math.max(2, MathHelper.ceil(minX / 0.25D));
            int maxY = Math.max(2, MathHelper.ceil(minY / 0.25D));
            int maxZ = Math.max(2, MathHelper.ceil(minZ / 0.25D));

            for (int x = 0; x < maxX; ++x) {
                for (int y = 0; y < maxY; ++y) {
                    for (int z = 0; z < maxZ; ++z) {
                        double vX = (x + 0.5D) / maxX;
                        double vY = (y + 0.5D) / maxY;
                        double vZ = (z + 0.5D) / maxZ;
                        double pX = vX * minX + x0;
                        double pY = vY * minY + y0;
                        double pZ = vZ * minZ + z0;
                        BlockDustParticle particle = new BlockDustParticle(
                            world, pos.getX() + pX, pos.getY() + pY, pos.getZ() + pZ, vX - 0.5D, vY - 0.5D, vZ - 0.5D,
                            state
                        );
                        particle.setBlockPos(pos);
                        if (sprite != null) {
                            particle.setSprite(new SingleSpriteProvider(sprite));
                        }
                        manager.addParticle(particle);
                    }
                }
            }
        }
    }

    protected final void addRequiredPart(AbstractPart required) {
        holder.addRequiredPart(required);
    }

    protected final void removeRequiredPart(AbstractPart required) {
        holder.removeRequiredPart(required);
    }

    /** @return The {@link VoxelShape} to use for calculating if this pluggable overlaps with another pluggable. */
    public abstract VoxelShape getShape();

    /** Checks to see if this {@link AbstractPart} can overlap with the other part. Note that this is only called if the
     * {@link #getShape()} of this part intersects with the {@link #getShape()} of the other part. However this is never
     * called if the shape of one of the parts is completely contained by the shape of the other part.
     * <p>
     * This is called once for each part currently contained in a {@link MultipartContainer} in
     * {@link MultipartContainer#offerNewPart(MultipartCreator, boolean)}. */
    public boolean canOverlapWith(AbstractPart other) {
        return false;
    }

    /** @return The shape to use when solidity logic, collisions with entities, ray tracing, etc. This should always
     *         encompass {@link #getShape()}. */
    public VoxelShape getCollisionShape() {
        return getShape();
    }

    /** @return The shape to use when calculating lighting and checking for opacity. This may be empty, although it
     *         should always be contained by {@link #getCollisionShape()}. Generally anything that's not opaque should
     *         override this and return {@link VoxelShapes#empty()}. */
    public VoxelShape getCullingShape() {
        return getCollisionShape();
    }

    /** @return The shape for rendering bounding boxes and ray tracing. */
    public VoxelShape getOutlineShape() {
        return getCollisionShape();
    }

    /** @return The (potentially dynamic) shape for rendering bounding boxes and ray tracing. Unlike
     *         {@link #getOutlineShape()} this is only called when rendering the box for this specific part. */
    public VoxelShape getDynamicShape(float partialTicks) {
        return getOutlineShape();
    }

    /** @return True if this pluggable should be an {@link AttributeList#obstruct(VoxelShape) obstacle} for attributes
     *         with it's {@link #getShape()} when searching in this particular direction. */
    public boolean isBlocking(Direction searchDirection) {
        return true;
    }

    /** Offers every contained attribute to the given attribute list. NOTE: This must always use
     * {@link AttributeList#offer(Object, VoxelShape)} with {@link #getShape()} as the {@link VoxelShape} argument!
     * <p>
     * 
     * @implNote This will {@link AttributeList#obstruct(VoxelShape)} the {@link #getShape()} if
     *           {@link #isBlocking(Direction)} returns true, and the search direction is not null. */
    public void addAllAttributes(AttributeList<?> list) {
        Direction searchDirection = list.getSearchDirection();
        if (searchDirection != null && isBlocking(searchDirection)) {
            list.obstruct(getShape());
        }
    }

    /** Called whenever this part is picked by the player (similar to
     * {@link Block#getPickStack(BlockView, BlockPos, BlockState)})
     * 
     * @return The stack that should be picked, or ItemStack.EMPTY if no stack can be picked from this pluggable. */
    public ItemStack getPickStack() {
        return ItemStack.EMPTY;
    }

    public void addDrops(DefaultedList<ItemStack> to) {
        ItemStack pickStack = getPickStack();
        if (!pickStack.isEmpty()) {
            to.add(pickStack);
        }
    }

    /** Called whenever this part is used via
     * {@link Block#onUse(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult)}. */
    public ActionResult onUse(PlayerEntity player, Hand hand, BlockHitResult hit) {
        return ActionResult.PASS;
    }

    /** Called whenever {@link BlockEntity#applyRotation(BlockRotation)} is called on the containing block.
     * 
     * @param rotation A rotation. LMP never calls this with {@link BlockRotation#NONE} */
    public void rotate(BlockRotation rotation) {

    }

    /** Called whenever {@link BlockEntity#applyMirror(BlockMirror)} is called on the containing block.
     * 
     * @param mirror A mirror. LMP never calls this with {@link BlockMirror#NONE} */
    public void mirror(BlockMirror mirror) {

    }

    /** Called on the client for both rendering, and checking if this needs to re-render in
     * {@link MultipartContainer#redrawIfChanged()}.
     * <p>
     * This is no longer called on the server.
     * 
     * @return The {@link PartModelKey} for the {@link PartModelBaker} to use to emit a static model. Returning null
     *         will bake nothing. */
    @Nullable
    public abstract PartModelKey getModelKey();
}
