package alexiil.mc.lib.multipart.api.misc;

import java.util.function.DoubleSupplier;

/** {@link Float} version of {@link DoubleSupplier}. */
@FunctionalInterface
public interface FloatSupplier {
    float getAsFloat();
}
