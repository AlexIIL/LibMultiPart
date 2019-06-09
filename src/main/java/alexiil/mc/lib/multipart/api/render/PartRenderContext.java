package alexiil.mc.lib.multipart.api.render;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import net.minecraft.client.render.model.BakedModel;

public interface PartRenderContext extends RenderContext {

    /** @return The backing {@link RenderContext} that is used for this. */
    RenderContext getRealRenderContext();

    /** @return A {@link PartBreakContext} if a part (or sub-part) is being broken. */
    @Nullable
    PartBreakContext getBreakContext();

    // RenderContext delegates

    @Override
    default Consumer<Mesh> meshConsumer() {
        return getRealRenderContext().meshConsumer();
    }

    @Override
    default Consumer<BakedModel> fallbackConsumer() {
        return getRealRenderContext().fallbackConsumer();
    }

    @Override
    default QuadEmitter getEmitter() {
        return getRealRenderContext().getEmitter();
    }

    @Override
    default void pushTransform(QuadTransform transform) {
        getRealRenderContext().pushTransform(transform);
    }

    @Override
    default void popTransform() {
        getRealRenderContext().popTransform();
    }
}
