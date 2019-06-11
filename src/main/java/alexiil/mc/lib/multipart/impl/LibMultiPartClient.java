package alexiil.mc.lib.multipart.impl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.render.BlockEntityRendererRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;

import alexiil.mc.lib.multipart.impl.client.model.MultiPartModel;
import alexiil.mc.lib.multipart.impl.client.model.PreBakedModel;
import alexiil.mc.lib.multipart.impl.client.render.MultiPartBlockEntityRenderer;
import alexiil.mc.lib.multipart.mixin.api.IWorldRendererMixin;

public class LibMultiPartClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LibMultiPart.isWorldClientPredicate = w -> w != null && w == MinecraftClient.getInstance().world;
        LibMultiPart.partialTickGetter = MinecraftClient.getInstance()::getTickDelta;
        LibMultiPart.isDrawingBlockOutlines = () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            return mc.isOnThread() && ((IWorldRendererMixin) mc.worldRenderer).libmultipart_isDrawingBlockOutline();
        };
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(res -> (id, ctx) -> getModelForVariant(id));
        BlockEntityRendererRegistry.INSTANCE.register(MultiPartBlockEntity.class, new MultiPartBlockEntityRenderer());
    }

    private static UnbakedModel getModelForVariant(ModelIdentifier id) {
        if (LibMultiPart.NAMESPACE.equals(id.getNamespace())) {
            if ("libmultipart:container#".equals(id.toString())) {
                return new PreBakedModel(new MultiPartModel());
            }
            LibMultiPart.LOGGER.warn("[client.model] Unknown model variant request: " + id);
        }
        return null;
    }
}
