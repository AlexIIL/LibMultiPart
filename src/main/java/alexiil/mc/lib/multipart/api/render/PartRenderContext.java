/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import net.minecraft.client.render.model.BakedModel;

public interface PartRenderContext extends RenderContext {

    /** @return The backing {@link RenderContext} that is used for this. */
    RenderContext getRealRenderContext();

    /** @return A {@link PartBreakContext} if a part (or sub-part) is being broken. */
    @Nullable
    PartBreakContext getBreakContext();

    /** @return True if the quads emitted to this render context should be pre-lit according to their direction (for
     *         block models) or not (for item models). */
    boolean shouldQuadsBeLit();

    // RenderContext delegates

    @Override
    default Consumer<Mesh> meshConsumer() {
        return getRealRenderContext().meshConsumer();
    }

    @Override
    default Consumer<BakedModel> fallbackConsumer() {
        return getRealRenderContext().fallbackConsumer();
    }

    @Override
    default QuadEmitter getEmitter() {
        return getRealRenderContext().getEmitter();
    }

    @Override
    default void pushTransform(QuadTransform transform) {
        getRealRenderContext().pushTransform(transform);
    }

    @Override
    default void popTransform() {
        getRealRenderContext().popTransform();
    }
}
