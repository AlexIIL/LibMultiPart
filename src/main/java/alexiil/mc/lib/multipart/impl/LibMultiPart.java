/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import alexiil.mc.lib.multipart.api.misc.FloatSupplier;

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
            callBreakByHand( // 1.18.1 compat method call
            FabricBlockSettings.of(material)//
                .dropsNothing()//
                .hardness(0.5f)//
                .resistance(2.0f)//
                .dynamicBounds()//
                .ticksRandomly()//
                .luminance(state -> state.get(MultipartBlock.LUMINANCE))
            )// 1.18.1 compat method exit
        );

        BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(MultipartBlockEntity::new, BLOCK).build();
        // TODO: Add a debug item (or command?) to view "missing part" information
    }

    @Override
    public void onInitialize() {
        // For now always register everything
        register(Registry.BLOCK, BLOCK, "container");
        register(Registry.BLOCK_ENTITY_TYPE, BLOCK_ENTITY, "container");

        MultipartBlockEntity.init();
    }

    private static <T> void register(Registry<T> registry, T obj, String path) {
        Registry.register(registry, id(path), obj);
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }

    /** Calls "FabricBlockSettings.breakByHand(true)", if it is present. This is part of the 1.18.1 compat code, since
     * tool attributes were removed from fabric-api in 1.18.2. This is expected to be removed in 1.19.0. */
    private static FabricBlockSettings callBreakByHand(FabricBlockSettings settings) {
        try {
            Method method = settings.getClass().getMethod("breakByHand", boolean.class);
            method.invoke(settings, true);
        } catch (ReflectiveOperationException e) {
            // Ignored
        }
        return settings;
    }
}
