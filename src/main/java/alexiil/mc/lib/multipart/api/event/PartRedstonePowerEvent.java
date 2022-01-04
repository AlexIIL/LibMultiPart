package alexiil.mc.lib.multipart.api.event;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

import alexiil.mc.lib.multipart.api.property.MultipartProperties;
import alexiil.mc.lib.multipart.impl.LmpInternalAccessible;

/** Fired during {@link Block#getStrongRedstonePower(BlockState, BlockView,BlockPos, Direction)} and
 * {@link Block#getWeakRedstonePower(BlockState, BlockView, BlockPos, Direction)}, however the more specific classes
 * {@link PartStrongRedstonePowerEvent} and {@link PartWeakRedstonePowerEvent} are only fired during their respective
 * method calls. */
public abstract class PartRedstonePowerEvent extends MultipartEvent {

    @LmpInternalAccessible
    static final PartRedstonePowerEventFactory STRONG_FACTORY = PartStrongRedstonePowerEvent::new;

    @LmpInternalAccessible
    static final PartRedstonePowerEventFactory WEAK_FACTORY = PartWeakRedstonePowerEvent::new;

    /** The value in {@link MultipartProperties#getWeakRedstonePower(Direction)} (and
     * {@link MultipartProperties#getStrongRedstonePower(Direction)} if this {@link #isStrong()}). */
    public final int powerProperty;

    /** @return The side of the block that is being */
    public final Direction side;

    @LmpInternalAccessible
    int value;

    private PartRedstonePowerEvent(int powerProperty, Direction side) {
        this.powerProperty = powerProperty;
        this.side = side;
        this.value = powerProperty;
    }

    public void set(int value) {
        if (value < 0 || value >= 16) {
            throw new IllegalArgumentException("value out of bounds!");
        }

        this.value = Math.max(value, this.value);
    }

    public abstract boolean isStrong();

    public static final class PartStrongRedstonePowerEvent extends PartRedstonePowerEvent {

        private PartStrongRedstonePowerEvent(int powerProperty, Direction side) {
            super(powerProperty, side);
        }

        @Override
        public boolean isStrong() {
            return true;
        }
    }

    public static final class PartWeakRedstonePowerEvent extends PartRedstonePowerEvent {

        private PartWeakRedstonePowerEvent(int powerProperty, Direction side) {
            super(powerProperty, side);
        }

        @Override
        public boolean isStrong() {
            return false;
        }
    }

    /** Event factory. Not part of the public api, since using it means LMP can't add any fields in the future. */
    @FunctionalInterface
    public interface PartRedstonePowerEventFactory {
        PartRedstonePowerEvent create(int fromProperty, Direction side);
    }
}
