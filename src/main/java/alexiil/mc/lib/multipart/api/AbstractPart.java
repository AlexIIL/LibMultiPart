/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api;

import java.util.Random;
import java.util.function.Function;

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
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.NetIdTyped;
import alexiil.mc.lib.net.ParentNetIdSingle;

import alexiil.mc.lib.attributes.AttributeList;

import alexiil.mc.lib.multipart.api.MultipartContainer.MultipartCreator;
import alexiil.mc.lib.multipart.api.event.EventListener;
import alexiil.mc.lib.multipart.api.render.PartModelBaker;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import alexiil.mc.lib.multipart.impl.LmpInternalOnly;
import alexiil.mc.lib.multipart.impl.PartContainer;
import alexiil.mc.lib.multipart.impl.SingleReplacementBlockView;
import alexiil.mc.lib.multipart.impl.client.SingleSpriteProvider;
import alexiil.mc.lib.multipart.mixin.impl.BlockSoundGroupAccessor;

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
    public static final NetIdDataK<AbstractPart> NET_SPAWN_FALL_PARTICLES;

    static {
        NET_ID = PartContainer.NET_KEY_PART;
        NET_RENDER_DATA = NET_ID.idData("render_data").toClientOnly()
            .setReadWrite(AbstractPart::readRenderData, AbstractPart::writeRenderData);
        // never buffer break particles because otherwise the block will be removed before the particles can spawn
        NET_SPAWN_BREAK_PARTICLES = NET_ID.idSignal("spawn_break_particles").toClientOnly().withoutBuffering()
            .setReceiver(AbstractPart::spawnBreakParticles);
        NET_SPAWN_FALL_PARTICLES = NET_ID.idData("spawn_fall_particles").toClientOnly()
            .setReceiver(AbstractPart::spawnFallParticles);
    }

    public final PartDefinition definition;
    public final MultipartHolder holder;

    public AbstractPart(PartDefinition definition, MultipartHolder holder) {
        this.definition = definition;
        this.holder = holder;
    }

    public NbtCompound toTag() {
        return new NbtCompound();
    }

    /** Writes the payload that will be passed into
     * {@link PartDefinition#loadFromBuffer(MultipartHolder, NetByteBuf, IMsgReadCtx)} on the client. (This is called on
     * the server and sent to the client). Note that this will be called *instead* of write and read payload. */
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

    /** @return The {@link BlockState} to use for {@link #playBreakSound()}, {@link #playHitSound(PlayerEntity)},
     *         {@link #spawnBreakParticles()}, {@link #spawnHitParticle(Direction)}, and
     *         {@link #calculateBreakingDelta(PlayerEntity)}. */
    protected BlockState getClosestBlockState() {
        return Blocks.DIRT.getDefaultState();
    }

    /** Called instead of {@link Block#onBreak(World, BlockPos, BlockState, PlayerEntity)}, to play the broken sound,
     * and spawn break particles.
     *
     * @return True if this should prevent {@link Block#onBreak(World, BlockPos, BlockState, PlayerEntity)} from being
     *         called afterwards, false otherwise. */
    @Environment(EnvType.CLIENT)
    public void playHitSound(PlayerEntity player) {
        playHitSound(getClosestBlockState());
    }

    /** Called by default in {@link #onBreak(PlayerEntity)} to play the breaking sound. The default implementation calls
     * {@link #playBreakSound(BlockState)} with {@link #getClosestBlockState()}. */
    protected void playBreakSound() {
        playBreakSound(getClosestBlockState());
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

    @Environment(EnvType.CLIENT)
    protected final void playHitSound(BlockState blockState) {
        BlockSoundGroup group = blockState.getSoundGroup();
        MinecraftClient.getInstance().getSoundManager().play(
            new PositionedSoundInstance(
                ((BlockSoundGroupAccessor) group).libmultipart_getHitSound(), SoundCategory.BLOCKS, //
                (group.getVolume() + 1.0F) / 8.0F, group.getPitch() * 0.8F, holder.getContainer().getMultipartPos()
            )
        );
    }

    private final void spawnBreakParticles(IMsgReadCtx ctx) throws InvalidInputDataException {
        ctx.assertClientSide();
        spawnBreakParticles();
    }

    /** Spawns a single breaking particle.
     *
     * @param side The side that was hit
     * @return True to cancel the default breaking particle from spawning, false otherwise.
     * @deprecated This was renamed to {@link #spawnHitParticle(Direction)} */
    @Environment(EnvType.CLIENT)
    @Deprecated
    public boolean spawnBreakingParticles(Direction side) {
        return spawnHitParticle(side);
    }

    /** Spawns a single partial-break (hit) particle.
     *
     * @param side The side that was hit
     * @return True to cancel the default breaking particle from spawning, false otherwise. */
    @Environment(EnvType.CLIENT)
    public boolean spawnHitParticle(Direction side) {
        spawnHitParticle(side, getClosestBlockState());
        return true;
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnHitParticle(Direction side, BlockState state) {
        spawnHitParticle(side, state, (Sprite) null);
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnHitParticle(Direction side, BlockState state, @Nullable Identifier spriteId) {
        Sprite sprite;
        if (spriteId == null) {
            sprite = null;
        } else {
            sprite = getBlockAtlas().apply(spriteId);
        }
        spawnHitParticle(side, state, sprite);
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnHitParticle(Direction side, BlockState state, @Nullable Sprite sprite) {
        spawnHitParticle(side, getOutlineShape().getBoundingBox(), state, sprite);
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnHitParticle(Direction side, Box box, BlockState state, @Nullable Sprite sprite) {
        World world = holder.getContainer().getMultipartWorld();
        BlockPos pos = holder.getContainer().getMultipartPos();
        ParticleManager manager = MinecraftClient.getInstance().particleManager;

        double x = pos.getX() + box.minX + pos(world, side, Direction.Axis.X, box.maxX - box.minX);
        double y = pos.getY() + box.minY + pos(world, side, Direction.Axis.Y, box.maxY - box.minY);
        double z = pos.getZ() + box.minZ + pos(world, side, Direction.Axis.Z, box.maxZ - box.minZ);

        BlockDustParticle particle = new BlockDustParticle((ClientWorld) world, x, y, z, 0, 0, 0, state, pos);
        particle.move(0.2f);
        particle.scale(0.6f);
        if (sprite != null) {
            particle.setSprite(new SingleSpriteProvider(sprite));
        }
        manager.addParticle(particle);
    }

    protected static final double pos(World world, Direction side, Direction.Axis axis, double size) {

        if (side.getAxis() == axis) {
            if (side.getDirection() == AxisDirection.NEGATIVE) {
                return -0.1;
            } else {
                return size + 0.1;
            }
        }

        if (size >= 0.5) {
            return 0.1 + (size - 0.2) * world.random.nextDouble();
        } else {
            double off = size / 8;
            return off + (size - 2 * off) * world.random.nextDouble();
        }
    }

    private static Function<Identifier, Sprite> getBlockAtlas() {
        return MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
    }

    /** Called on the client to spawn break particles. This calls {@link #spawnBreakParticles(BlockState)} with
     * {@link #getClosestBlockState()} by default. */
    @Environment(EnvType.CLIENT)
    protected void spawnBreakParticles() {
        spawnBreakParticles(getClosestBlockState());
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
            sprite = getBlockAtlas().apply(spriteId);
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
            double x0 = box.minX;
            double y0 = box.minY;
            double z0 = box.minZ;
            double x1 = box.maxX;
            double y1 = box.maxY;
            double z1 = box.maxZ;
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
                            (ClientWorld) world, pos.getX() + pX, pos.getY() + pY, pos.getZ() + pZ, vX - 0.5D,
                            vY - 0.5D, vZ - 0.5D, state, pos
                        );
                        if (sprite != null) {
                            particle.setSprite(new SingleSpriteProvider(sprite));
                        }
                        manager.addParticle(particle);
                    }
                }
            }
        }
    }

    /** Spawns a single sprint particle.
     *
     * @param sprintingEntity The entity doing the sprinting.
     * @param entityRandom The entity's random for use in particle position &amp; velocity calculations.
     * @return True to cancel the default sprinting particle from spawning, false otherwise. */
    @Environment(EnvType.CLIENT)
    public boolean spawnSprintParticle(Entity sprintingEntity, Random entityRandom) {
        spawnSprintParticle(sprintingEntity, entityRandom, getClosestBlockState());
        return true;
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnSprintParticle(Entity sprintingEntity, Random entityRandom, BlockState state) {
        spawnSprintParticle(sprintingEntity, entityRandom, state, (Sprite) null);
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnSprintParticle(Entity sprintingEntity, Random entityRandom, BlockState state, @Nullable Identifier spriteId) {
        Sprite sprite;
        if (spriteId == null) {
            sprite = null;
        } else {
            sprite = getBlockAtlas().apply(spriteId);
        }
        spawnSprintParticle(sprintingEntity, entityRandom, state, sprite);
    }

    @Environment(EnvType.CLIENT)
    protected final void spawnSprintParticle(Entity sprintingEntity, Random entityRandom, BlockState state, @Nullable Sprite sprite) {
        World world = holder.getContainer().getMultipartWorld();
        BlockPos blockPos = holder.getContainer().getMultipartPos();
        ParticleManager manager = MinecraftClient.getInstance().particleManager;

        Vec3d velocity = sprintingEntity.getVelocity();
        double width = sprintingEntity.getWidth();

        double x = sprintingEntity.getX() + (entityRandom.nextDouble() - 0.5) * width;
        double y = sprintingEntity.getY() + 0.1;
        double z = sprintingEntity.getZ() + (entityRandom.nextDouble() - 0.5) * width;
        double dx = velocity.x * -4.0;
        double dy = 1.5;
        double dz = velocity.z * -4.0;

        BlockDustParticle particle = new BlockDustParticle((ClientWorld) world, x, y, z, dx, dy, dz, state, blockPos);
        if (sprite != null) {
            particle.setSprite(new SingleSpriteProvider(sprite));
        }
        manager.addParticle(particle);
    }

    /** Spawns a single particle for when an iron golem walks on this part.
     *
     * @param ironGolem The iron golem doing the walking.
     * @param entityRandom The iron golem's random for use in particle position &amp; velocity calculations.
     * @return True to cancel the default iron golem walking particle from spawning, false otherwise. */
    @Environment(EnvType.CLIENT)
    public boolean spawnIronGolemParticle(IronGolemEntity ironGolem, Random entityRandom) {
        spawnIronGolemParticle(ironGolem, entityRandom, getClosestBlockState());
        return true;
    }

    protected final void spawnIronGolemParticle(IronGolemEntity ironGolem, Random entityRandom, BlockState state) {
        spawnIronGolemParticle(ironGolem, entityRandom, state, (Sprite) null);
    }

    protected final void spawnIronGolemParticle(IronGolemEntity ironGolem, Random entityRandom, BlockState state, @Nullable Identifier spriteId) {
        Sprite sprite;
        if (spriteId == null) {
            sprite = null;
        } else {
            sprite = getBlockAtlas().apply(spriteId);
        }
        spawnIronGolemParticle(ironGolem, entityRandom, state, sprite);
    }

    protected final void spawnIronGolemParticle(IronGolemEntity ironGolem, Random entityRandom, BlockState state, @Nullable Sprite sprite) {
        World world = holder.getContainer().getMultipartWorld();
        BlockPos blockPos = holder.getContainer().getMultipartPos();
        ParticleManager manager = MinecraftClient.getInstance().particleManager;

        double width = ironGolem.getWidth();

        double x = ironGolem.getX() + (entityRandom.nextDouble() - 0.5) * width;
        double y = ironGolem.getY() + 0.1;
        double z = ironGolem.getZ() + (entityRandom.nextDouble() - 0.5) * width;
        double dx = (entityRandom.nextDouble() - 0.5) * 4.0;
        double dy = 0.5;
        double dz = (entityRandom.nextDouble() - 0.5) * 4.0;

        BlockDustParticle particle = new BlockDustParticle((ClientWorld) world, x, y, z, dx, dy, dz, state, blockPos);
        if (sprite != null) {
            particle.setSprite(new SingleSpriteProvider(sprite));
        }
        manager.addParticle(particle);
    }

    /** Called on the server when an entity has fallen on this part and is attempting to spawn particles for it.
     *
     * This is to send packets to clients to actually spawn the particles.
     *
     * @param fallenEntity The entity that has just landed on this part.
     * @param entityRandom The entity's random for use in particle position &amp; velocity calculations.
     * @return True to cancel the default fall particle from spawning, false otherwise. */
    public boolean onSpawnFallParticles(LivingEntity fallenEntity, Random entityRandom) {
        float f = MathHelper.ceil(fallenEntity.fallDistance - 3.0f);
        double d = Math.min(0.2f + f / 15.0f, 2.5);
        int count = (int)(150.0 * d);
        sendSpawnFallParticles(fallenEntity.getPos(), count);
        return true;
    }

    /** Actually Sends the packet from the server to the clients that spawns the fall particles.
     *
     * @param pos The position of the particles to spawn.
     * @param count The number of particles to spawn. */
    protected final void sendSpawnFallParticles(Vec3d pos, int count) {
        sendNetworkUpdate(this, NET_SPAWN_FALL_PARTICLES, (obj, buffer, ctx) -> {
            ctx.assertServerSide();
            buffer.writeDouble(pos.x);
            buffer.writeDouble(pos.y);
            buffer.writeDouble(pos.z);
            buffer.writeInt(count);
        });
    }

    private void spawnFallParticles(NetByteBuf buf, IMsgReadCtx ctx) {
        Vec3d pos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int count = buf.readInt();
        spawnFallParticles(pos, count);
    }

    /** Called on the client to spawn fall particles.
     *
     * By default, this calls {@link #spawnFallParticles(Vec3d, int, BlockState)} with {@link #getClosestBlockState()}
     * as its {@link BlockState} parameter.
     *
     * @param pos The position of the particles to spawn.
     * @param count The number of particles to spawn. */
    protected void spawnFallParticles(Vec3d pos, int count) {
        spawnFallParticles(pos, count, getClosestBlockState());
    }

    protected final void spawnFallParticles(Vec3d pos, int count, BlockState state) {
        spawnFallParticles(pos, count, state, (Sprite) null);
    }

    protected final void spawnFallParticles(Vec3d pos, int count, BlockState state, @Nullable Identifier spriteId) {
        Sprite sprite;
        if (spriteId == null) {
            sprite = null;
        } else {
            sprite = getBlockAtlas().apply(spriteId);
        }
        spawnFallParticles(pos, count, state, sprite);
    }

    protected final void spawnFallParticles(Vec3d pos, int count, BlockState state, @Nullable Sprite sprite) {
        World world = holder.getContainer().getMultipartWorld();
        BlockPos blockPos = holder.getContainer().getMultipartPos();
        Random random = world.random;
        ParticleManager manager = MinecraftClient.getInstance().particleManager;

        for (int i = 0; i < count; i++) {
            double dx = random.nextGaussian() * 0.15;
            double dy = random.nextGaussian() * 0.15;
            double dz = random.nextGaussian() * 0.15;
            BlockDustParticle particle = new BlockDustParticle(
                    (ClientWorld) world, pos.getX(), pos.getY(), pos.getZ(), dx, dy, dz, state, blockPos
            );
            if (sprite != null) {
                particle.setSprite(new SingleSpriteProvider(sprite));
            }
            manager.addParticle(particle);
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
     *         {@link #getOutlineShape()} this is only called when rendering the box for this specific part.
     * @deprecated Use (and implement) {@link #getDynamicShape(float, Vec3d)} instead. */
    @Deprecated
    public VoxelShape getDynamicShape(float partialTicks) {
        return getOutlineShape();
    }

    /** @param hitVec The exact position of the main block where... oh.
     * @return The (potentially dynamic) shape for rendering bounding boxes and ray tracing. Unlike
     *         {@link #getOutlineShape()} this is only called when rendering the box for this specific part. */
    public VoxelShape getDynamicShape(float partialTicks, Vec3d hitVec) {
        return getDynamicShape(partialTicks);
    }

    /** @return True if this pluggable should be an {@link AttributeList#obstruct(VoxelShape) obstacle} for attributes
     *         with it's {@link #getShape()} when searching in this particular direction. */
    public boolean isBlocking(Direction searchDirection) {
        return true;
    }

    /** Offers every contained attribute to the given attribute list. NOTE: This must always use
     * {@link AttributeList#offer(Object, VoxelShape)} with {@link #getShape()} as the {@link VoxelShape} argument!
     * <p>
     * The default implementation will {@link AttributeList#obstruct(VoxelShape)} the {@link #getShape()} if
     * {@link #isBlocking(Direction)} returns true, and the search direction is not null. */
    public void addAllAttributes(AttributeList<?> list) {
        Direction searchDirection = list.getSearchDirection();
        if (searchDirection != null && isBlocking(searchDirection)) {
            list.obstruct(getShape());
        }
    }

    /** Called whenever this part is picked by the player (similar to
     * {@link Block#getPickStack(BlockView, BlockPos, BlockState)})
     *
     * @return The stack that should be picked, or ItemStack.EMPTY if no stack can be picked from this part. */
    public ItemStack getPickStack() {
        return ItemStack.EMPTY;
    }

    /** A target for retrieving item drops from {@link AbstractPart#addDrops(ItemDropTarget, LootContext)}. This will
     * either spawn the dropped items into the world, or add them to a list, depending on what method called it. */
    public interface ItemDropTarget {

        /** Drops the given itemstack, using a default position and velocity. */
        void drop(ItemStack stack);

        default void dropAll(Iterable<ItemStack> iter) {
            for (ItemStack stack : iter) {
                drop(stack);
            }
        }

        /** Drops the given {@link ItemStack}, using the given position and a random velocity. This will also split the
         * stack up randomly, like how vanilla splits stacks. */
        void drop(ItemStack stack, Vec3d pos);

        /** If {@link #dropsAsEntity()} is false then this ignores the position and velocity. Otherwise, this drops the
         * given {@link ItemStack} directly at the given position, using the exact given velocity. This won't split the
         * stack up. */
        void drop(ItemStack stack, Vec3d pos, Vec3d velocity);

        /** @return True if the position and velocity of the {@link ItemStack} will be used. */
        boolean dropsAsEntity();
    }

    public void addDrops(ItemDropTarget target, LootContext context) {
        DefaultedList<ItemStack> list = DefaultedList.of();
        addDrops(list);
        if (!list.isEmpty()) {
            target.dropAll(list);
        }
    }

    /** @deprecated Replaced by {@link #addDrops(ItemDropTarget, LootContext)} */
    @Deprecated
    public void addDrops(DefaultedList<ItemStack> to) {
        ItemStack pickStack = getPickStack();
        if (!pickStack.isEmpty()) {
            to.add(pickStack);
        }
    }

    /** Called instead of {@link Block#afterBreak(World, PlayerEntity, BlockPos, BlockState, BlockEntity, ItemStack)},
     * except that this shouldn't drop any items, as that's handled separately. */
    public void afterBreak(PlayerEntity player) {
        player.addExhaustion(0.005F);
    }

    /** Part version of {@link Block#calcBlockBreakingDelta(BlockState, PlayerEntity, BlockView, BlockPos)}.
     * <p>
     * The default implementation treats parts as equal to {@link #getClosestBlockState()}. */
    public float calculateBreakingDelta(PlayerEntity player) {
        return calculateBreakingDelta(player, getClosestBlockState());
    }

    /** Calculates {@link #calculateBreakingDelta(PlayerEntity)} as if this part was the given block instead. */
    public final float calculateBreakingDelta(PlayerEntity player, Block block) {
        return calculateBreakingDelta(player, block.getDefaultState());
    }

    /** Calculates {@link #calculateBreakingDelta(PlayerEntity)} as if this part was the given block state instead. */
    public final float calculateBreakingDelta(PlayerEntity player, BlockState state) {
        World world = holder.getContainer().getMultipartWorld();
        BlockPos thisPos = holder.getContainer().getMultipartPos();
        float hardness = state.getHardness(new SingleReplacementBlockView(world, thisPos, state), thisPos);
        return calcBreakingDelta(player, state, hardness);
    }

    /** Calculates {@link #calculateBreakingDelta(PlayerEntity)} as if this part was the given block state instead, but
     * using a custom hardness value. */
    public static float calcBreakingDelta(PlayerEntity player, BlockState state, float hardness) {
        if (hardness == -1.0F) {
            return 0.0F;
        } else {
            int mult = player.canHarvest(state) ? 30 : 100;
            return player.getBlockBreakingSpeed(state) / hardness / mult;
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

    // ############
    // # Internal #
    // ############

    /** Workaround for {@link #playBreakSound()} being protected, but
     * {@link MultipartHolder#remove(MultipartHolder.PartRemoval...)} needing to call it. */
    @LmpInternalOnly
    private final void callPlayBreakSound() {
        playBreakSound();
    }
}
