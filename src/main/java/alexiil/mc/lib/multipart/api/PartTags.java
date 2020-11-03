package alexiil.mc.lib.multipart.api;

import alexiil.mc.lib.multipart.api.PartTags.CachingTag;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloadListener;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroupLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

/** {@link PartDefinition} equivalent to {@link BlockTags}. */
public final class PartTags {
    private PartTags() {}

    private static TagGroupLoader<PartDefinition> container = new TagGroupLoader<>(id -> Optional.empty(), "", false, "");
    private static int reloadCount = 0;

    public static ResourceReloadListener reloader() {
        return new ResourceReloadListener() {
            @Override
            public CompletableFuture<Void> reload(Synchronizer sync, ResourceManager manager, Profiler prof1,
                Profiler prof2, Executor ex1, Executor ex2) {

            }
        };
    }

    public static TagGroupLoader<PartDefinition> getContainer() {
        return container;
    }

    /** @return A caching tag that is always valid - so you can safely store this in a static variable. */
    public static Tag<PartDefinition> getTag(Identifier id) {
        return new CachingTag(id);
    }

    static class CachingTag extends Tag<PartDefinition> {
        private int reloads = -1;
        private Tag<PartDefinition> delegate;

        public CachingTag(Identifier identifier_1) {
            super(identifier_1);
        }

        private Tag<PartDefinition> delegate() {
            if (reloads != reloadCount) {
                delegate = container.getOrCreate(this.getId());
                reloads = reloadCount;
            }
            return delegate;
        }

        @Override
        public boolean contains(PartDefinition def) {
            return delegate().contains(def);
        }

        @Override
        public Collection<PartDefinition> values() {
            return delegate().values();
        }

        @Override
        public Collection<Tag.Entry<PartDefinition>> entries() {
            return delegate().entries();
        }
    }
}
