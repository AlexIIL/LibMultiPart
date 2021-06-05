/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartHolder;

/** Combined {@link BlockPos} + {@link PartHolder#uniqueId}. */
final class PosPartId {

    private static final Set<String> OLD_EXACT_TAG_NAMES = new HashSet<>(Arrays.asList("x", "y", "z", "u"));
    private static final Set<String> NEW_OFFSET_TAG_NAMES = new HashSet<>(Arrays.asList("a", "b", "c", "u"));

    final BlockPos pos;
    final long uid;

    PosPartId(BlockPos pos, long uid) {
        this.pos = pos.toImmutable();
        this.uid = uid;
    }

    PosPartId(AbstractPart part) {
        this(part.holder);
    }

    PosPartId(MultipartHolder holder) {
        this(holder.getContainer().getMultipartPos(), holder.getUniqueId());
    }

    PosPartId(PartContainer from, NbtCompound tag) {
        if (tag.contains("x")) {
            pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        } else {
            BlockPos f = from.getMultipartPos();
            pos = new BlockPos(tag.getInt("a") + f.getX(), tag.getInt("b") + f.getY(), tag.getInt("c") + f.getZ());
        }
        uid = tag.getLong("u");
    }

    NbtCompound toTag(PartContainer from) {
        BlockPos f = from.getMultipartPos();
        NbtCompound tag = new NbtCompound();
        tag.putInt("a", pos.getX() - f.getX());
        tag.putInt("b", pos.getY() - f.getY());
        tag.putInt("c", pos.getZ() - f.getZ());
        tag.putLong("u", uid);
        return tag;
    }

    public static boolean isValid(NbtCompound tag) {
        Set<String> keys = tag.getKeys();
        return keys.size() == 4 && (keys.containsAll(OLD_EXACT_TAG_NAMES) || keys.containsAll(NEW_OFFSET_TAG_NAMES));
    }

    @Override
    public String toString() {
        return "{ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " #" + uid + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj.getClass() != getClass()) {
            return false;
        }
        PosPartId other = (PosPartId) obj;
        return uid == other.uid && pos.equals(other.pos);
    }

    @Override
    public int hashCode() {
        // 524287 = 2 ^ 19 - 1
        long lval = (((uid * 524287 + pos.getX()) * 31) + pos.getY()) * 31 + pos.getZ();
        return Long.hashCode(lval);
    }

    public boolean posEquals(BlockPos otherPos) {
        return pos.equals(otherPos);
    }

    public boolean isFor(PartHolder req) {
        return uid == req.uniqueId && posEquals(req.container.getMultipartPos());
    }

    public PosPartId rotate(PartContainer from, BlockRotation rotation) {
        if (rotation == BlockRotation.NONE) {
            return this;
        }
        BlockPos f = from.getMultipartPos();
        return new PosPartId(pos.subtract(f).rotate(rotation).add(f), uid);
    }

    public PosPartId mirror(PartContainer from, BlockMirror mirror) {
        if (mirror == BlockMirror.NONE) {
            return this;
        }
        BlockPos f = from.getMultipartPos();
        int x = pos.getX() - f.getX();
        int z = pos.getZ() - f.getZ();
        if (mirror == BlockMirror.LEFT_RIGHT) {
            x = -x;
        } else {
            assert mirror == BlockMirror.FRONT_BACK;
            z = -z;
        }
        return new PosPartId(new BlockPos(x + f.getX(), pos.getY(), z + f.getZ()), uid);
    }
}
