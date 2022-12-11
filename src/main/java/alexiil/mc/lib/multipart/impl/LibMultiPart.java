/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import alexiil.mc.lib.multipart.api.misc.FloatSupplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;

public class LibMultiPart implements ModInitializer {

    public static final String NAMESPACE = "libmultipart";
    public static final Logger LOGGER = LogManager.getLogger(NAMESPACE);
    public static final boolean DEBUG = Boolean.getBoolean("libmultipart.debug");

    public static final MultipartBlock BLOCK;
    public static final BlockEntityType<MultipartBlockEntity> BLOCK_ENTITY;

    static Predicate<World> isWorldClientPredicate = w -> false;
    static FloatSupplier partialTickGetter = () -> 1;

    static {
        if (DEBUG) {
            LOGGER.info(
                "Debugging enabled for LibMultiPart - you can disable it by adding '-Dlibmultipart.debug=false' to your launch args"
            );
        } else {
            LOGGER.debug(
                "Debugging not enabled for LibMultiPart - you can enable it by adding '-Dlibmultipart.debug=true' to your launch args"
            );
        }

        Material material = new Material.Builder(MapColor.BLACK).build();
        BLOCK = new MultipartBlock(
            FabricBlockSettings.of(material)//
                .dropsNothing()//
                .hardness(0.5f)//
                .resistance(2.0f)//
                .dynamicBounds()//
                .ticksRandomly()//
                .luminance(state -> state.get(MultipartBlock.LUMINANCE))
        );

        BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(MultipartBlockEntity::new, BLOCK).build();
        // TODO: Add a debug item (or command?) to view "missing part" information
    }

    @Override
    public void onInitialize() {
        // For now always register everything
        register(Registries.BLOCK, BLOCK, "container");
        register(Registries.BLOCK_ENTITY_TYPE, BLOCK_ENTITY, "container");

        MultipartBlockEntity.init();
    }

    private static <T> void register(Registry<T> registry, T obj, String path) {
        Registry.register(registry, id(path), obj);
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }
}
