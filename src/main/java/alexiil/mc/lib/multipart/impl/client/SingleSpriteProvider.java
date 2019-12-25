/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client;

import java.util.Random;

import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.texture.Sprite;

public final class SingleSpriteProvider implements SpriteProvider {
    private final Sprite sprite;

    public SingleSpriteProvider(Sprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public Sprite getSprite(int i, int j) {
        return sprite;
    }

    @Override
    public Sprite getSprite(Random random) {
        return sprite;
    }
}
