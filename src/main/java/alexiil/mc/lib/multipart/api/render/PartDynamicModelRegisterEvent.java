/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import alexiil.mc.lib.multipart.api.AbstractPart;

@FunctionalInterface
public interface PartDynamicModelRegisterEvent {

    Event<PartDynamicModelRegisterEvent> EVENT
        = EventFactory.createArrayBacked(PartDynamicModelRegisterEvent.class, (listeners) -> (renderer) -> {
            for (PartDynamicModelRegisterEvent l : listeners) {
                l.registerModels(renderer);
            }
        });

    void registerModels(DynamicModelRenderer renderer);

    public interface DynamicModelRenderer {
        /** Registers a renderer that will render the given class, and all of it's subclasses (unless a different
         * renderer is registered for one of the subclasses).
         * <p>
         * Note that only a single renderer will ever be called. */
        <P extends AbstractPart> void register(Class<P> clazz, PartRenderer<P> renderer);
    }
}
