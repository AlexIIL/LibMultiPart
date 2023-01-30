/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.misc;

import javax.annotation.Nullable;

import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.DirectionTransformation;

/**
 * A handful of utilities for working with {@link DirectionTransformation}s.
 */
public final class DirectionTransformationUtil {
    private DirectionTransformationUtil() {
    }

    /**
     * Gets the equivalent {@link BlockRotation} for a given {@link DirectionTransformation} if any.
     *
     * @param transformation The transformation to try and turn into a BlockRotation.
     * @return The BlockRotation equivalent to the given transformation or <code>null</code> if the given transformation
     * has no equivalent BlockRotation.
     */
    @Nullable
    public static BlockRotation getRotation(DirectionTransformation transformation) {
        return switch (transformation) {
            case IDENTITY -> BlockRotation.NONE;
            case ROT_90_Y_NEG -> BlockRotation.CLOCKWISE_90;
            case ROT_180_FACE_XZ -> BlockRotation.CLOCKWISE_180;
            case ROT_90_Y_POS -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> null;
        };
    }

    /**
     * Gets the equivalent {@link BlockMirror} for a given {@link DirectionTransformation} if any.
     *
     * @param transformation The transformation to try and turn into a BlockMirror.
     * @return The BlockMirror equivalent to the given transformation or <code>null</code> if the given transformation
     * has no equivalent BlockMirror.
     */
    @Nullable
    public static BlockMirror getMirror(DirectionTransformation transformation) {
        // Note: BlockMirror.LEFT_RIGHT corresponds to DirectionTransformation.INVERT_Z and BlockMirror.FRONT_BACK
        // corresponds to DirectionTransformation.INVERT_X. This is NOT intuitive.
        return switch (transformation) {
            case IDENTITY -> BlockMirror.NONE;
            case INVERT_Z -> BlockMirror.LEFT_RIGHT;
            case INVERT_X -> BlockMirror.FRONT_BACK;
            default -> null;
        };
    }

    /**
     * Determines whether a {@link DirectionTransformation} can be represented as either a {@link BlockRotation} or
     * {@link BlockMirror}.
     */
    public static boolean isRotationOrMirror(DirectionTransformation transformation) {
        return switch (transformation) {
            case IDENTITY, ROT_90_Y_NEG, ROT_180_FACE_XZ, ROT_90_Y_POS, INVERT_Z, INVERT_X -> true;
            default -> false;
        };
    }

    /**
     * Gets the relative transformation that would need to be prepended to the base transformation to obtain the new
     * transformation.
     *
     * @param base              The transformation we're relativizing the new transformation in relation to.
     * @param newTransformation The transformation we're turning into a relative transformation.
     * @return A relative transformation that could be prepended to <code>base</code> to obtain
     * <code>newTransformation</code>.
     */
    public static DirectionTransformation getRelativeTransformation(DirectionTransformation base,
                                                                    DirectionTransformation newTransformation) {
        // 'A': the `base` transformation
        // 'X': delta transformation prepended to the base to obtain new `newTransformation`
        // 'A^-1': A.inverse()
        //
        // Currently `newTransformation` hold the value 'XA' but we really only want to find X.
        // To find X, we just prepend the `newTransformation` to 'A^-1' to get: 'XAA^-1' = 'X'.
        return base.inverse().prepend(newTransformation);
    }
}
