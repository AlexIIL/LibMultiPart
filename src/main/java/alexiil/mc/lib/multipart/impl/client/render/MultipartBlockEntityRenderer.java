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
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.PartDynamicModelRegisterEvent;
import alexiil.mc.lib.multipart.api.render.PartRenderer;
import alexiil.mc.lib.multipart.impl.MultipartBlockEntity;
import alexiil.mc.lib.multipart.impl.PartHolder;

public class MultipartBlockEntityRenderer implements BlockEntityRenderer<MultipartBlockEntity> {
    private final Map<Class<? extends AbstractPart>, PartRenderer<?>> renderers = new HashMap<>();
    private final Map<Class<? extends AbstractPart>, PartRenderData<?>> resolved = new HashMap<>();

    public MultipartBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        PartDynamicModelRegisterEvent.EVENT.invoker().registerModels(new PartDynamicModelRegisterEvent.DynamicModelRenderer() {
            @Override
            public <P extends AbstractPart> void register(Class<P> clazz, PartRenderer<P> renderer) {
                renderers.put(clazz, renderer);
                resolved.clear();
            }
        });
    }

    private <P extends AbstractPart> PartRenderData<? super P> getRenderer(Class<P> clazz) {
        PartRenderData<?> renderer = resolved.get(clazz);
        if (renderer != null) {
            return (PartRenderData<? super P>) renderer;
        }

        Class<? super P> c = clazz;
        do {
            PartRenderer<?> pr = renderers.get(c);
            if (pr != null) {
                PartRenderData<?> data = new PartRenderData<>(pr, getDebugName(clazz));
                resolved.put(clazz, data);
                return (PartRenderData<? super P>) data;
            }
        } while ((c = c.getSuperclass()) != null);
        PartRenderData<AbstractPart> data = new PartRenderData<>(NoopRenderer.INSTANCE, getDebugName(clazz));
        resolved.put(clazz, data);
        return data;
    }

    private static String getDebugName(Class<?> clazz) {
        Package pkg = clazz.getPackage();
        String fqn = clazz.getName();

        if (pkg == null) {
            return fqn;
        }

        if (fqn.startsWith(pkg.getName() + ".")) {
            return fqn.substring(pkg.getName().length() + 1);
        } else {
            return fqn;
        }

    }

    @Override
    public void render(
        MultipartBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
        int light, int overlay
    ) {
        Profiler p = MinecraftClient.getInstance().getProfiler();

        p.push("LMP");
        for (PartHolder holder : be.getContainer().parts) {
            renderPart(p, holder.part, holder.part.getClass(), tickDelta, matrices, vertexConsumers, light, overlay);
        }
        p.pop();
    }

    <P extends AbstractPart> void renderPart(
        Profiler p, AbstractPart part, Class<P> clazz, float tickDelta, MatrixStack matrices,
        VertexConsumerProvider vertexConsumers, int light, int overlay
    ) {
        PartRenderData<? super P> data = getRenderer(clazz);
        p.push(data.debugName);
        data.renderer.render(clazz.cast(part), tickDelta, matrices, vertexConsumers, light, overlay);
        p.pop();
    }

    enum NoopRenderer implements PartRenderer<AbstractPart> {
        INSTANCE;

        @Override
        public void render(
            AbstractPart part, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
            int overlay
        ) {
            // NO-OP
        }
    }

    static class PartRenderData<P extends AbstractPart> {
        final PartRenderer<P> renderer;
        final String debugName;

        PartRenderData(PartRenderer<P> renderer, String debugName) {
            this.renderer = renderer;
            this.debugName = debugName;
        }
    }
}
