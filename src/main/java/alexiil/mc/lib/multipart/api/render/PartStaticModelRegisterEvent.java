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

import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

@FunctionalInterface
public interface PartStaticModelRegisterEvent {

    Event<PartStaticModelRegisterEvent> EVENT
        = EventFactory.createArrayBacked(PartStaticModelRegisterEvent.class, (listeners) -> (renderer) -> {
            for (PartStaticModelRegisterEvent l : listeners) {
                l.registerModels(renderer);
            }
        });

    void registerModels(StaticModelRenderer renderer);

    public interface StaticModelRenderer {
        <K extends PartModelKey> void register(Class<K> clazz, PartModelBaker<K> renderer);

        // Sprites

        default Sprite getMissingBlockSprite() {
            return getSprite(SpriteAtlasTexture.BLOCK_ATLAS_TEX, MissingSprite.getMissingSpriteId());
        }

        default Sprite getBlockSprite(String id) {
            return getSprite(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier(id));
        }

        default Sprite getBlockSprite(Identifier id) {
            return getSprite(SpriteAtlasTexture.BLOCK_ATLAS_TEX, id);
        }

        Sprite getSprite(Identifier atlasId, Identifier spriteId);

        default Sprite getSprite(SpriteIdentifier spriteId) {
            return getSprite(spriteId.getAtlasId(), spriteId.getTextureId());
        }
    }
}
