package alexiil.mc.lib.multipart.api.event;

import net.minecraft.world.biome.Biome;

/** Fired once per {@linkplain net.minecraft.block.Block#precipitationTick(net.minecraft.block.BlockState, net.minecraft.world.World, net.minecraft.util.math.BlockPos, net.minecraft.world.biome.Biome.Precipitation)
 * precipitation tick.} */
public final class PartPrecipitationTickEvent extends MultipartEvent {
    public final Biome.Precipitation precipitation;

    public PartPrecipitationTickEvent(Biome.Precipitation precipitation) {
        this.precipitation = precipitation;
    }
}
