/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.model;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.PartBreakContext;
import alexiil.mc.lib.multipart.api.render.PartRenderContext;

public final class BreakingPartRenderContext implements PartRenderContext, PartBreakContext {

    public final RenderContext renderContext;

    public final AbstractPart breakPart;
    public final Object breakSubPart;

    public BreakingPartRenderContext(RenderContext renderContext, AbstractPart breakPart, Object breakSubPart) {
        this.renderContext = renderContext;
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
    public AbstractPart getPart() {
        return breakPart;
    }

    @Override
    public Object getSubPart() {
        return breakSubPart;
    }
}
