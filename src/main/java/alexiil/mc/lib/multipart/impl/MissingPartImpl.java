/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.function.Function;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import alexiil.mc.lib.multipart.api.MissingPart;
import alexiil.mc.lib.multipart.api.MissingPartDefinition;
import alexiil.mc.lib.multipart.api.MultipartHolder;
import alexiil.mc.lib.multipart.api.render.PartModelKey;

public final class MissingPartImpl extends MissingPart {

    private static final Function<Object[], MissingPartDefinition> MISSING_PART_DEF_CTOR
        = LmpReflection.getApiConstructor(MissingPartDefinition.class, Identifier.class);

    static final MissingPartDefinition DEF_NOT_AN_ID
        = MISSING_PART_DEF_CTOR.apply(new Object[] { new Identifier("libmultipart", "unknown/not_an_id") });

    final String originalId;
    final NbtCompound originalNbt;

    MissingPartImpl(MultipartHolder holder, String originalId, NbtCompound originalNbt) {
        super(createDefinition(originalId), holder);
        this.originalId = originalId;
        this.originalNbt = originalNbt;
    }

    private static MissingPartDefinition createDefinition(String originalId) {
        Identifier id = Identifier.tryParse(originalId);
        if (id != null) {
            return MISSING_PART_DEF_CTOR.apply(new Object[] { id });
        } else {
            return DEF_NOT_AN_ID;
        }
    }

    @Override
    public VoxelShape getShape() {
        return VoxelShapes.empty();
    }

    @Override
    public PartModelKey getModelKey() {
        // TODO: If we have a shape then emit with this!
        return null;
    }
}
