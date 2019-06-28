/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.model;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.MultipartRenderRegistry;
import alexiil.mc.lib.multipart.api.render.PartModelBaker;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import alexiil.mc.lib.multipart.api.render.PartRenderContext;
import alexiil.mc.lib.multipart.impl.MultipartBlockEntity;
import alexiil.mc.lib.multipart.impl.PartContainer;
import alexiil.mc.lib.multipart.impl.TransientPartIdentifier;
import alexiil.mc.lib.multipart.mixin.api.IClientPlayerInteractionManagerMixin;
import alexiil.mc.lib.multipart.mixin.api.IWorldRendererMixin;

public class MultipartModel implements BakedModel, FabricBakedModel {

    // BakedModel

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction dir, Random rand) {
        return MinecraftClient.getInstance().getBakedModelManager().getMissingModel().getQuads(state, dir, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean hasDepthInGui() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getSprite() {
        return MissingSprite.getMissingSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelItemPropertyOverrideList getItemPropertyOverrides() {
        return ModelItemPropertyOverrideList.EMPTY;
    }

    // FabricBakedModel

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(ExtendedBlockView blockView, BlockState state, BlockPos pos, Supplier<
        Random> randomSupplier, RenderContext context) {

        BlockEntity be = blockView.getBlockEntity(pos);
        if (be instanceof MultipartBlockEntity) {
            PartContainer container = ((MultipartBlockEntity) be).getContainer();

            MinecraftClient mc = MinecraftClient.getInstance();
            if (
                mc.isOnThread()
                && ((IWorldRendererMixin) mc.worldRenderer).libmultipart_isRenderingPartiallyBrokenBlocks()
            ) {
                IClientPlayerInteractionManagerMixin interactionManager
                    = (IClientPlayerInteractionManagerMixin) mc.interactionManager;
                if (pos.equals(interactionManager.libmultipart_getCurrentBreakPosition())) {
                    Object partKey = interactionManager.libmultipart_getPartKey();
                    if (partKey instanceof TransientPartIdentifier) {
                        TransientPartIdentifier identifier = (TransientPartIdentifier) partKey;
                        AbstractPart part = identifier.part;
                        PartModelKey modelKey = part.getModelKey();
                        BreakingPartRenderContext partContext = new BreakingPartRenderContext(
                            context, part, identifier.subPart
                        );
                        emitQuads(modelKey, modelKey.getClass(), partContext);
                        return;
                    }
                }
            }

            PartRenderContext ctx = new NormalPartRenderContext(context);
            for (PartModelKey key : container.getPartModelKeys()) {
                emitQuads(key, key.getClass(), ctx);
            }
        }
    }

    private static <K extends PartModelKey> void emitQuads(PartModelKey key, Class<K> clazz,
        PartRenderContext context) {
        PartModelBaker<? super K> baker = MultipartRenderRegistry.getBaker(clazz);
        if (baker != null) {
            baker.emitQuads(clazz.cast(key), context);
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        // This isn't used for item models.
    }
}
