/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import net.minecraft.util.math.DirectionTransformation;

import alexiil.mc.lib.multipart.api.misc.DirectionTransformationUtil;

/**
 * Used to check if all parts in a multipart block support a type of transformation.
 * <p>
 * If any parts mark the given transformation as invalid, then the given transformation will not be applied to any part
 * in the multipart block.
 *
 * @see PartTransformEvent
 */
public class PartTransformCheckEvent extends MultipartEvent {
    public final DirectionTransformation transformation;

    private boolean invalid = false;

    public PartTransformCheckEvent(DirectionTransformation transformation) {
        this.transformation = transformation;
    }

    /**
     * Returns whether a part has already marked this transformation as invalid.
     */
    public boolean isInvalid() {
        return invalid;
    }

    /**
     * Marks this transformation as invalid.
     * <p>
     * If any parts mark the given transformation as invalid, then the given transformation will not be applied to any
     * part in the multipart block.
     */
    public void markInvalid() {
        invalid = true;
    }

    /**
     * Marks this transformation as invalid if it cannot be represented as a {@link net.minecraft.util.BlockRotation}
     * or {@link net.minecraft.util.BlockMirror}.
     */
    public void allowOnlyRotationOrMirror() {
        invalid = invalid || !DirectionTransformationUtil.isRotationOrMirror(transformation);
    }
}
