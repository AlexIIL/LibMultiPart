/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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

    public static final MultipartBlock BLOCK;
    public static final BlockEntityType<MultipartBlockEntity> BLOCK_ENTITY;

    static Predicate<World> isWorldClientPredicate = w -> false;
    static BooleanSupplier isDrawingBlockOutlines = () -> false;
    static FloatSupplier partialTickGetter = () -> 1;

    static {
        Material material = new Material.Builder(MaterialColor.BLACK).build();
        BLOCK = new MultipartBlock(
            FabricBlockSettings.of(material).dropsNothing().breakByHand(true).hardness(0.5f).resistance(2.0f).build()
        );

        Set<Block> blocks = Collections.singleton(BLOCK);
        BLOCK_ENTITY = new BlockEntityType<>(MultipartBlockEntity::new, blocks, null);
    }

    @Override
    public void onInitialize() {
        // For now always register everything
        register(Registry.BLOCK, BLOCK, "container");
        register(Registry.BLOCK_ENTITY, BLOCK_ENTITY, "container");
    }

    private static <T> void register(Registry<T> registry, T obj, String path) {
        Registry.register(registry, new Identifier(NAMESPACE, path), obj);
    }
}
