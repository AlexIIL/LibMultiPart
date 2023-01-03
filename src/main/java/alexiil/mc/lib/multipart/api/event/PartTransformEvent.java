/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.api.event;

import alexiil.mc.lib.multipart.api.misc.DirectionTransformationUtil;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.DirectionTransformation;

/** Indicates that the given part, along with all other parts in the multipart block, are being transformed by the given
 * transformation.
 * <p>
 * This transformation can represent any kind of axis-aligned rotation or mirror, not being confined to just
 * {@link BlockRotation} or {@link BlockMirror} which have no effect on the y-axis. However, there are sub-events for
 * when the transformation is specifically equivalent to either a {@link BlockRotation} or a {@link BlockMirror}.
 * <p>
 * Transformations can be applied to this part's block via
 * {@link net.minecraft.block.BlockState#mirror(BlockMirror)},
 * {@link net.minecraft.block.BlockState#rotate(BlockRotation)},
 * {@link net.minecraft.block.entity.BlockEntity#applyRotation(BlockRotation)},
 * {@link net.minecraft.block.entity.BlockEntity#applyMirror(BlockMirror)},
 * {@link net.minecraft.block.entity.BlockEntity#applyTransformation(DirectionTransformation)}.
 *
 * @see PartTransformCheckEvent
 * @see PartPreTransformEvent
 * @see PartPostTransformEvent
 * @see DirectionTransformationUtil */
public class PartTransformEvent extends MultipartEvent {
    public final DirectionTransformation transformation;

    private PartTransformEvent(DirectionTransformation transformation) {
        this.transformation = transformation;
    }

    /** Represents a transformation event for only a simple {@link BlockRotation}.
     *
     * @see PartTransformEvent */
    public static class Rotate extends PartTransformEvent {
        public final BlockRotation rotation;

        private Rotate(BlockRotation rotation) {
            super(rotation.getDirectionTransformation());
            this.rotation = rotation;
        }
    }

    /** Represents a transformation event for only a simple {@link BlockMirror}.
     *
     * @see PartTransformEvent */
    public static class Mirror extends PartTransformEvent {
        public final BlockMirror mirror;

        private Mirror(BlockMirror mirror) {
            super(mirror.getDirectionTransformation());
            this.mirror = mirror;
        }
    }

    /** Constructs the correct subclass for the given transformation.
     * <p>
     * If the given transformation can be represented as an equivalent {@link BlockRotation}, then a {@link Rotate}
     * event is returned. If the given transformation can be represented as an equivalent {@link BlockMirror}, then a
     * {@link Mirror} event is returned. If the given transformation cannot be represented as either a
     * {@link BlockRotation} or a {@link BlockMirror} then only a base {@link PartTransformEvent} is returned. */
    public static PartTransformEvent create(DirectionTransformation transformation) {
        if (transformation == DirectionTransformation.IDENTITY) {
            throw new IllegalArgumentException("A PartTransformEvent for an IDENTITY transformation is useless");
        }

        BlockRotation rotation = DirectionTransformationUtil.getRotation(transformation);
        BlockMirror mirror = DirectionTransformationUtil.getMirror(transformation);

        if (rotation != null) {
            return new Rotate(rotation);
        } else if (mirror != null) {
            return new Mirror(mirror);
        } else {
            return new PartTransformEvent(transformation);
        }
    }
}
