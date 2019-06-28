/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.function.Consumer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.render.BlockEntityRendererRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;

import alexiil.mc.lib.multipart.impl.client.model.MultipartModel;
import alexiil.mc.lib.multipart.impl.client.model.PreBakedModel;
import alexiil.mc.lib.multipart.impl.client.render.MultipartBlockEntityRenderer;
import alexiil.mc.lib.multipart.mixin.api.IWorldRendererMixin;

public class LibMultiPartClient implements ClientModInitializer {

    public static final ModelIdentifier MODEL_CANNOT_FIND_MASTER;

    static {
        MODEL_CANNOT_FIND_MASTER = new ModelIdentifier("libmultipart:block/cannot_find_master#");
    }

    @Override
    public void onInitializeClient() {
        LibMultiPart.isWorldClientPredicate = w -> w != null && w == MinecraftClient.getInstance().world;
        LibMultiPart.partialTickGetter = MinecraftClient.getInstance()::getTickDelta;
        LibMultiPart.isDrawingBlockOutlines = () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            return mc.isOnThread() && ((IWorldRendererMixin) mc.worldRenderer).libmultipart_isDrawingBlockOutline();
        };
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(res -> (id, ctx) -> getModelForVariant(id));
        ModelLoadingRegistry.INSTANCE.registerAppender(LibMultiPartClient::requestModels);
        BlockEntityRendererRegistry.INSTANCE.register(MultipartBlockEntity.class, new MultipartBlockEntityRenderer());
    }

    private static void requestModels(ResourceManager res, Consumer<ModelIdentifier> out) {
        out.accept(MODEL_CANNOT_FIND_MASTER);
    }

    private static UnbakedModel getModelForVariant(ModelIdentifier id) {
        if (LibMultiPart.NAMESPACE.equals(id.getNamespace())) {
            if ("libmultipart:container#".equals(id.toString())) {
                return new PreBakedModel(new MultipartModel());
            }
            LibMultiPart.LOGGER.warn("[client.model] Unknown model variant request: " + id);
            return null;
        }
        return null;
    }
}
