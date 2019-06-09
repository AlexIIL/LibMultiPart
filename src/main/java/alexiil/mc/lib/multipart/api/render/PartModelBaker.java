package alexiil.mc.lib.multipart.api.render;

public interface PartModelBaker<K extends PartModelKey> {
    void emitQuads(K key, PartRenderContext ctx);
}
