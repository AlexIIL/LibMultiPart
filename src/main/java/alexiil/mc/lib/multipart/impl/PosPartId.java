package alexiil.mc.lib.multipart.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartHolder;

/** Combined {@link BlockPos} + {@link PartHolder#uniqueId}. */
final class PosPartId {

    private static final Set<String> TAG_NAMES = new HashSet<>(Arrays.asList("x", "y", "z", "u"));

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

    PosPartId(CompoundTag tag) {
        pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        uid = tag.getLong("u");
    }

    CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putLong("u", uid);
        return tag;
    }

    public static boolean isValid(CompoundTag tag) {
        return tag.getKeys().size() == 4//
            && tag.getKeys().containsAll(TAG_NAMES);
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
}
