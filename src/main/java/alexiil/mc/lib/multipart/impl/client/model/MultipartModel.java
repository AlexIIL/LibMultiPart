/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.PartModelBaker;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import alexiil.mc.lib.multipart.api.render.PartRenderContext;
import alexiil.mc.lib.multipart.api.render.PartStaticModelRegisterEvent;
import alexiil.mc.lib.multipart.impl.MultipartBlockEntity;
import alexiil.mc.lib.multipart.impl.PartContainer;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier.IdAdditional;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier.IdSubPart;
import alexiil.mc.lib.multipart.impl.client.PartModelData;
import alexiil.mc.lib.multipart.mixin.api.IBlockRenderManagerMixin;
import alexiil.mc.lib.multipart.mixin.api.IClientPlayerInteractionManagerMixin;

public final class MultipartModel
    implements BakedModel, FabricBakedModel, PartStaticModelRegisterEvent.StaticModelRenderer {

    private final Map<Class<? extends PartModelKey>, PartModelBaker<?>> bakers, resolved;
    private final Function<SpriteIdentifier, Sprite> textureGetter;

    public MultipartModel(Function<SpriteIdentifier, Sprite> textureGetter) {
        this.textureGetter = textureGetter;
        bakers = new HashMap<>();
        resolved = new HashMap<>();
        PartStaticModelRegisterEvent.EVENT.invoker().registerModels(this);
    }

    @Override
    public <P extends PartModelKey> void register(Class<P> clazz, PartModelBaker<P> renderer) {
        bakers.put(clazz, renderer);
        resolved.clear();
    }

    @Override
    public Sprite getSprite(Identifier atlasId, Identifier spriteId) {
        return textureGetter.apply(new SpriteIdentifier(atlasId, spriteId));
    }

    public <P extends PartModelKey> PartModelBaker<? super P> getBaker(Class<P> clazz) {
        PartModelBaker<?> baker = resolved.get(clazz);
        if (baker != null) {
            return (PartModelBaker<? super P>) baker;
        }

        Class<? super P> c = clazz;
        do {
            PartModelBaker<?> pb = bakers.get(c);
            if (pb != null) {
                resolved.put(clazz, pb);
                return (PartModelBaker<? super P>) pb;
            }
        } while ((c = c.getSuperclass()) != null);
        resolved.put(clazz, NoopBaker.INSTANCE);
        return NoopBaker.INSTANCE;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction dir, Random rand) {
        return MinecraftClient.getInstance().getBakedModelManager().getMissingModel().getQuads(state, dir, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return true;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelManager().getMissingModel()
            .getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }

    // FabricBakedModel

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(
        BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier,
        RenderContext context
    ) {
        BlockEntity be = blockView.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            PartContainer container = ((MultipartBlockEntity) be).getContainer();

            MinecraftClient mc = MinecraftClient.getInstance();
            if (
                mc.isOnThread() && ((IBlockRenderManagerMixin) mc.getBlockRenderManager())
                    .libmultipart_isRenderingPartiallyBrokenBlocks()
            ) {
                IClientPlayerInteractionManagerMixin interactionManager
                    = (IClientPlayerInteractionManagerMixin) mc.interactionManager;
                if (pos.equals(interactionManager.libmultipart_getCurrentBreakPosition())) {
                    Object partKey = interactionManager.libmultipart_getPartKey();
                    if (partKey instanceof TransientPartIdentifier) {
                        TransientPartIdentifier identifier = (TransientPartIdentifier) partKey;
                        AbstractPart part = identifier.part;
                        PartModelKey modelKey = part.getModelKey();
                        Object subPart = null;
                        if (identifier.extra instanceof IdSubPart<?>) {
                            subPart = ((IdSubPart<?>) identifier.extra).subpart;
                        }
                        BreakingPartRenderContext partContext
                            = new BreakingPartRenderContext(context, false, randomSupplier, part, subPart);
                        if (modelKey != null) {
                            emitQuads(modelKey, modelKey.getClass(), partContext);
                        }
                        if (identifier.extra instanceof IdAdditional) {
                            for (AbstractPart additional : ((IdAdditional) identifier.extra).additional) {
                                PartModelKey key2 = additional.getModelKey();
                                if (key2 != null) {
                                    emitQuads(key2, key2.getClass(), partContext);
                                }
                            }
                        }
                        return;
                    }
                }
            }

            PartRenderContext ctx = new NormalPartRenderContext(context, false, randomSupplier);
            ImmutableList<PartModelKey> keys = container.getPartModelKeys();
            if (blockView instanceof RenderAttachedBlockView) {
                Object data = ((RenderAttachedBlockView) blockView).getBlockEntityRenderAttachment(pos);
                if (data instanceof PartModelData) {
                    keys = ((PartModelData) data).keys;
                }
            }
            for (PartModelKey key : keys) {
                emitQuads(key, key.getClass(), ctx);
            }
        }
    }

    private <K extends PartModelKey> void emitQuads(PartModelKey key, Class<K> clazz, PartRenderContext context) {
        PartModelBaker<? super K> baker = getBaker(clazz);
        if (baker != null) {
            baker.emitQuads(clazz.cast(key), context);
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        // This isn't used for item models.
    }

    enum NoopBaker implements PartModelBaker<PartModelKey> {
        INSTANCE;

        @Override
        public void emitQuads(PartModelKey key, PartRenderContext ctx) {
            // NO-OP
        }
    }

    public static final class Unbaked implements UnbakedModel {
        MultipartModel model;

        @Override
        public Collection<Identifier> getModelDependencies() {
            return Collections.emptySet();
        }

        @Override
        public Collection<SpriteIdentifier> getTextureDependencies(
            Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences
        ) {
            return Collections.emptySet();
        }

        @Override
        public BakedModel bake(
            ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer,
            Identifier modelId
        ) {
            if (model == null) {
                model = new MultipartModel(textureGetter);
            }
            return model;
        }
    }
}
