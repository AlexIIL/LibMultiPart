/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;

import net.minecraft.client.render.block.entity.BlockEntityRenderer;

import alexiil.mc.lib.multipart.impl.MultiPartBlockEntity;

public class MultiPartBlockEntityRenderer extends BlockEntityRenderer<MultiPartBlockEntity> {
    @Override
    public void render(MultiPartBlockEntity be, double x, double y, double z, float partialTicks, int breakProgress) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        assert renderer != null;
        // TODO: Dynamic rendering!
    }
}
