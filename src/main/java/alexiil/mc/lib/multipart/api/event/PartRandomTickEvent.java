package alexiil.mc.lib.multipart.api.event;

/** Fired once per {@linkplain net.minecraft.block.BlockState#randomTick(net.minecraft.server.world.ServerWorld, net.minecraft.util.math.BlockPos, java.util.Random)
 * random tick.} */
public final class PartRandomTickEvent extends MultipartEvent implements ContextlessEvent {
    public static final PartRandomTickEvent INSTANCE = new PartRandomTickEvent();

    private PartRandomTickEvent() {}
}
