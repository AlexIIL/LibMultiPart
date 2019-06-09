package alexiil.mc.lib.multipart.api.render;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public final class MultiPartRenderRegistry {
    private MultiPartRenderRegistry() {}

    private static final Map<Class<? extends PartModelKey>, PartModelBaker<?>> bakers = new HashMap<>();

    public static <K extends PartModelKey> void registerBaker(Class<K> clazz, PartModelBaker<? super K> baker) {
        bakers.put(clazz, baker);
    }

    @Nullable
    public static <K extends PartModelKey> PartModelBaker<? super K> getBaker(Class<K> clazz) {
        PartModelBaker<?> baker = bakers.get(clazz);
        if (baker != null) {
            return (PartModelBaker<? super K>) baker;
        }
        return null;
    }
}
