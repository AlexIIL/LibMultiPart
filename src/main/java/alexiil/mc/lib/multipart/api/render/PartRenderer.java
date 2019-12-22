/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import alexiil.mc.lib.multipart.api.AbstractPart;

public interface PartRenderer<P extends AbstractPart> {
    void render(
        P part, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
        int breakProgress
    );
}
