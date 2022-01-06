/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.model;

import java.util.Random;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import alexiil.mc.lib.multipart.api.render.PartBreakContext;
import alexiil.mc.lib.multipart.api.render.PartRenderContext;

public final class NormalPartRenderContext implements PartRenderContext {

    public final RenderContext ctx;
    public final boolean shouldQuadsBeLit;
    public final Supplier<Random> random;

    public NormalPartRenderContext(RenderContext ctx, boolean shouldQuadsBeLit, Supplier<Random> random) {
        this.ctx = ctx;
        this.shouldQuadsBeLit = shouldQuadsBeLit;
        this.random = random;
    }

    @Override
    public RenderContext getRealRenderContext() {
        return ctx;
    }

    @Override
    public PartBreakContext getBreakContext() {
        return null;
    }

    @Override
    public boolean shouldQuadsBeLit() {
        return shouldQuadsBeLit;
    }

    @Override
    public Supplier<Random> getRandomSupplier() {
        return random;
    }
}
