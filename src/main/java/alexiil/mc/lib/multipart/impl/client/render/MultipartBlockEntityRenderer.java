/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.PartDynamicModelRegisterEvent;
import alexiil.mc.lib.multipart.api.render.PartRenderer;
import alexiil.mc.lib.multipart.impl.MultipartBlockEntity;
import alexiil.mc.lib.multipart.impl.PartHolder;
import alexiil.mc.lib.multipart.impl.client.model.MultipartModel;

public class MultipartBlockEntityRenderer extends BlockEntityRenderer<MultipartBlockEntity> {
    private static final Map<Class<? extends AbstractPart>, PartRenderer<?>> renderers, resolved;

    static {
        renderers = new HashMap<>();
        resolved = new HashMap<>();
    }

    public MultipartBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    /** Called by {@link MultipartModel} as the {@link BlockEntityRenderDispatcher} is a singleton, and so only creates
     * this renderer once. */
    public static void registerModels() {
        assert MinecraftClient.getInstance().isOnThread() : "Not on the main thread!";
        PartDynamicModelRegisterEvent.EVENT.invoker().registerModels(Registrar.INSTANCE);
    }

    private static <P extends AbstractPart> PartRenderer<? super P> getRenderer(Class<P> clazz) {
        PartRenderer<?> renderer = resolved.get(clazz);
        if (renderer != null) {
            return (PartRenderer<? super P>) renderer;
        }

        Class<? super P> c = clazz;
        do {
            PartRenderer<?> pr = renderers.get(c);
            if (pr != null) {
                resolved.put(clazz, pr);
                return (PartRenderer<? super P>) pr;
            }
        } while ((c = c.getSuperclass()) != null);
        resolved.put(clazz, NoopRenderer.INSTANCE);
        return NoopRenderer.INSTANCE;
    }

    @Override
    public void render(
        MultipartBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
        int light, int overlay
    ) {
        for (PartHolder holder : be.getContainer().parts) {
            AbstractPart part = holder.part;
            renderPart(part, part.getClass(), tickDelta, matrices, vertexConsumers, light, overlay);
        }
    }

    static <P extends AbstractPart> void renderPart(
        AbstractPart part, Class<P> clazz, float tickDelta, MatrixStack matrices,
        VertexConsumerProvider vertexConsumers, int light, int overlay
    ) {
        PartRenderer<? super P> renderer = getRenderer(clazz);
        if (renderer != null) {
            renderer.render(clazz.cast(part), tickDelta, matrices, vertexConsumers, light, overlay);
        }
    }

    enum NoopRenderer implements PartRenderer<AbstractPart> {
        INSTANCE;

        @Override
        public void render(
            AbstractPart part, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            int breakProgress
        ) {
            // NO-OP
        }
    }

    enum Registrar implements PartDynamicModelRegisterEvent.DynamicModelRenderer {
        INSTANCE;

        @Override
        public <P extends AbstractPart> void register(Class<P> clazz, PartRenderer<P> renderer) {
            renderers.put(clazz, renderer);
            resolved.clear();
        }
    }
}
