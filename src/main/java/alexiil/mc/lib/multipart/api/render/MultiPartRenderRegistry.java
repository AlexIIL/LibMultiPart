/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.render;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import alexiil.mc.lib.multipart.api.AbstractPart;

public final class MultiPartRenderRegistry {
    private MultiPartRenderRegistry() {}

    private static final Map<Class<? extends PartModelKey>, PartModelBaker<?>> bakers = new HashMap<>();
    private static final Map<Class<? extends AbstractPart>, PartRenderer<?>> renderers = new HashMap<>();

    public static <K extends PartModelKey> void registerBaker(Class<K> clazz, PartModelBaker<? super K> baker) {
        bakers.put(clazz, baker);
    }

    @Nullable
    public static <K extends PartModelKey> PartModelBaker<? super K> getBaker(Class<K> clazz) {
        PartModelBaker<?> baker = bakers.get(clazz);
        if (baker != null) {
            return (PartModelBaker<? super K>) baker;
        }
        return null;
    }

    public static <P extends AbstractPart> void registerRenderer(Class<P> clazz, PartRenderer<? super P> baker) {
        renderers.put(clazz, baker);
    }

    @Nullable
    public static <P extends AbstractPart> PartRenderer<? super P> getRenderer(Class<P> clazz) {
        PartRenderer<?> baker = renderers.get(clazz);
        if (baker != null) {
            return (PartRenderer<? super P>) baker;
        }
        return null;
    }
}
