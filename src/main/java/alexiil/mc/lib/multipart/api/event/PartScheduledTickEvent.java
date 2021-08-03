package alexiil.mc.lib.multipart.api.event;

/** Fired once per {@linkplain net.minecraft.block.BlockState#scheduledTick(net.minecraft.server.world.ServerWorld, net.minecraft.util.math.BlockPos, java.util.Random)
 * scheduled tick.} */
public final class PartScheduledTickEvent extends MultipartEvent implements ContextlessEvent {
    public static final PartScheduledTickEvent INSTANCE = new PartScheduledTickEvent();

    private PartScheduledTickEvent() {}
}
