/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.model;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import net.minecraft.util.math.random.Random;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.PartBreakContext;
import alexiil.mc.lib.multipart.api.render.PartRenderContext;

public final class BreakingPartRenderContext implements PartRenderContext, PartBreakContext {

    public final RenderContext renderContext;
    public final boolean shouldQuadsBeLit;
    public final Supplier<Random> random;

    public final AbstractPart breakPart;
    public final Object breakSubPart;

    public BreakingPartRenderContext(
        RenderContext renderContext, boolean shouldQuadsBeLit, Supplier<Random> random, AbstractPart breakPart,
        Object breakSubPart
    ) {
        this.renderContext = renderContext;
        this.shouldQuadsBeLit = shouldQuadsBeLit;
        this.random = random;
        this.breakPart = breakPart;
        this.breakSubPart = breakSubPart;
    }

    @Override
    public RenderContext getRealRenderContext() {
        return renderContext;
    }

    @Override
    public PartBreakContext getBreakContext() {
        return this;
    }

    @Override
    public boolean shouldQuadsBeLit() {
        return shouldQuadsBeLit;
    }

    @Override
    public Supplier<Random> getRandomSupplier() {
        return random;
    }

    @Override
    public AbstractPart getPart() {
        return breakPart;
    }

    @Override
    public Object getSubPart() {
        return breakSubPart;
    }
}
