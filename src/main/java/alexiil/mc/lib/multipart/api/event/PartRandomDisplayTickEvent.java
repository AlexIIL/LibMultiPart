package alexiil.mc.lib.multipart.api.event;

/** Fired once per {@linkplain net.minecraft.block.Block#randomDisplayTick(net.minecraft.block.BlockState, net.minecraft.world.World, net.minecraft.util.math.BlockPos, java.util.Random)
 * random display tick.} */
public final class PartRandomDisplayTickEvent extends MultipartEvent implements ContextlessEvent {
    public static final PartRandomDisplayTickEvent INSTANCE = new PartRandomDisplayTickEvent();

    private PartRandomDisplayTickEvent() {}
}
