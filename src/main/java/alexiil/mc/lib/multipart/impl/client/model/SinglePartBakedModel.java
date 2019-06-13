package alexiil.mc.lib.multipart.impl.client.model;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.render.MultiPartRenderRegistry;
import alexiil.mc.lib.multipart.api.render.PartModelBaker;
import alexiil.mc.lib.multipart.api.render.PartModelKey;

/** A {@link BakedModel} that wraps a single {@link AbstractPart}. */
public class SinglePartBakedModel<K extends PartModelKey> implements BakedModel, FabricBakedModel {

    public final K key;
    public final Class<K> clazz;

    public SinglePartBakedModel(K key, Class<K> clazz) {
        this.key = key;
        this.clazz = clazz;
    }

    public static SinglePartBakedModel<?> create(PartModelKey key) {
        return key == null ? new SinglePartBakedModel<>(null, PartModelKey.class) : create0(key, key.getClass());
    }

    private static <K extends PartModelKey> SinglePartBakedModel<?> create0(PartModelKey key, Class<K> clazz) {
        return new SinglePartBakedModel<>(clazz.cast(key), clazz);
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(ExtendedBlockView blockView, BlockState state, BlockPos pos, Supplier<
        Random> randomSupplier, RenderContext context) {
        emitQuads(context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        emitQuads(context);
    }

    private void emitQuads(RenderContext context) {
        if (key != null) {
            PartModelBaker<? super K> baker = MultiPartRenderRegistry.getBaker(clazz);
            if (baker != null) {
                baker.emitQuads(key, new NormalPartRenderContext(context));
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(BlockState var1, Direction var2, Random var3) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean hasDepthInGui() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getSprite() {
        return MissingSprite.getMissingSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelItemPropertyOverrideList getItemPropertyOverrides() {
        return ModelItemPropertyOverrideList.EMPTY;
    }
}
