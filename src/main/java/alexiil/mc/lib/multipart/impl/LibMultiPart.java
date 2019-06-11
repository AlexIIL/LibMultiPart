package alexiil.mc.lib.multipart.impl;

import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.block.FabricBlockSettings;

import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.api.misc.FloatSupplier;

public class LibMultiPart implements ModInitializer {

    public static final String NAMESPACE = "libmultipart";
    public static final Logger LOGGER = LogManager.getLogger(NAMESPACE);

    public static final MultiPartBlock BLOCK;
    public static final BlockEntityType<MultiPartBlockEntity> BLOCK_ENTITY;

    static Predicate<World> isWorldClientPredicate = w -> false;
    static BooleanSupplier isDrawingBlockOutlines = () -> false;
    static FloatSupplier partialTickGetter = () -> 1;

    static {
        Material material = new Material.Builder(MaterialColor.BLACK).build();
        BLOCK = new MultiPartBlock(
            FabricBlockSettings.of(material).dropsNothing().breakByHand(true).hardness(0.5f).resistance(2.0f).build()
        );

        Set<Block> blocks = Collections.singleton(BLOCK);
        BLOCK_ENTITY = new BlockEntityType<>(MultiPartBlockEntity::new, blocks, null);
    }

    @Override
    public void onInitialize() {
        register(Registry.BLOCK, BLOCK, "container");
        register(Registry.BLOCK_ENTITY, BLOCK_ENTITY, "container");
    }

    private static <T> void register(Registry<T> registry, T obj, String path) {
        Registry.register(registry, new Identifier(NAMESPACE, path), obj);
    }
}
