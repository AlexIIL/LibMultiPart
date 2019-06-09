package alexiil.mc.lib.multipart.impl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;

import alexiil.mc.lib.multipart.impl.client.model.MultiPartModel;
import alexiil.mc.lib.multipart.impl.client.model.PreBakedModel;

public class LibMultiPartClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LibMultiPart.isWorldClientPredicate = w -> w != null && w == MinecraftClient.getInstance().world;
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(res -> (id, ctx) -> getModelForVariant(id));
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
