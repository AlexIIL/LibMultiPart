/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.render;

import net.minecraft.client.render.block.entity.BlockEntityRenderer;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.MultipartRenderRegistry;
import alexiil.mc.lib.multipart.api.render.PartRenderer;
import alexiil.mc.lib.multipart.impl.MultipartBlockEntity;
import alexiil.mc.lib.multipart.impl.PartHolder;

public class MultipartBlockEntityRenderer extends BlockEntityRenderer<MultipartBlockEntity> {
    @Override
    public void render(MultipartBlockEntity be, double x, double y, double z, float partialTicks, int breakProgress) {
        for (PartHolder holder : be.getContainer().parts) {
            AbstractPart part = holder.part;
            renderPart(part, part.getClass(), x, y, z, partialTicks, breakProgress);
        }
    }

    static <P extends AbstractPart> void renderPart(AbstractPart part, Class<P> clazz, double x, double y, double z,
        float partialTicks, int breakProgress) {
        PartRenderer<? super P> renderer = MultipartRenderRegistry.getRenderer(clazz);
        if (renderer != null) {
            renderer.render(clazz.cast(part), x, y, z, partialTicks, breakProgress);
        }
    }
}
