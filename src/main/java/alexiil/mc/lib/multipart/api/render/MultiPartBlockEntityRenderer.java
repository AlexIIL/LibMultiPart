package alexiil.mc.lib.multipart.api.render;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;

import net.minecraft.client.render.block.entity.BlockEntityRenderer;

import alexiil.mc.lib.multipart.impl.MultiPartBlockEntity;

public class MultiPartBlockEntityRenderer extends BlockEntityRenderer<MultiPartBlockEntity> {
    @Override
    public void render(MultiPartBlockEntity be, double x, double y, double z, float partialTicks, int breakProgress) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        assert renderer != null;
        // TODO: Dynamic rendering!
    }
}
