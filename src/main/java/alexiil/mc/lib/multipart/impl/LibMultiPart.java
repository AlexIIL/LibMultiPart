/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

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
            FabricBlockSettings.of(material)//
                .dropsNothing()//
                .breakByHand(true)//
                .hardness(0.5f)//
                .resistance(2.0f)//
                .dynamicBounds()//
                .ticksRandomly()//
                .luminance(state -> state.get(MultipartBlock.LUMINANCE))
        );

        BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(MultipartBlockEntity::new, BLOCK).build();
    }

    @Override
    public void onInitialize() {
        // For now always register everything
        register(Registry.BLOCK, BLOCK, "container");
        register(Registry.BLOCK_ENTITY_TYPE, BLOCK_ENTITY, "container");
    }

    private static <T> void register(Registry<T> registry, T obj, String path) {
        Registry.register(registry, id(path), obj);
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }

    /** Retrieves a value from a static LMP field that is annotated with {@link LmpInternalAccessible}. */
    public static <T> T getStaticApiField(Class<?> from, String field, Class<T> fieldType) {
        try {
            Field fld = from.getField(field);

            if (fld.getAnnotation(LmpInternalAccessible.class) == null) {
                throw new Error(
                    "Tried to access a non-internally exposed field! (" + from + " ." + field + " of " + fieldType + ")"
                );
            }

            fld.setAccessible(true);
            checkType(from, field, fieldType, fld);
            if ((fld.getModifiers() & Modifier.STATIC) == 0) {
                throw new Error(
                    "LMP field is not static when we expected it to be static! (" + from + " ." + field + " of "
                        + fieldType + ")"
                );
            }
            return fieldType.cast(fld.get(null));
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new Error(
                "LMP failed to access it's own field?! (" + from + " ." + field + " of " + fieldType + ")", e
            );
        }
    }

    /** Retrieves a function that returns the value held in a non-static LMP field that is annotated with
     * {@link LmpInternalAccessible}. */
    public static <C, F> Function<C, F> getInstanceApiField(Class<C> from, String field, Class<F> fieldType) {
        try {
            Field fld = from.getField(field);

            if (fld.getAnnotation(LmpInternalAccessible.class) == null) {
                throw new Error(
                    "Tried to access a non-internally exposed field! (" + from + " ." + field + " of " + fieldType + ")"
                );
            }

            fld.setAccessible(true);
            checkType(from, field, fieldType, fld);
            if ((fld.getModifiers() & Modifier.STATIC) == 0) {
                throw new Error(
                    "LMP field is static when we expected it not to be! (" + from + " ." + field + " of " + fieldType
                        + ")"
                );
            }

            return instance -> {
                try {
                    return fieldType.cast(fld.get(instance));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new Error(
                        "LMP failed to access it's own field?! (" + from + " ." + field + " of " + fieldType + ")", e
                    );
                }
            };
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new Error(
                "LMP failed to access it's own field?! (" + from + " ." + field + " of " + fieldType + ")", e
            );
        }
    }

    private static void checkType(Class<?> from, String field, Class<?> fieldType, Field fld) throws Error {

        if (fld.getType().isPrimitive() && !fieldType.isPrimitive()) {
            if (fld.getType() == Character.TYPE) fieldType = Character.class;
            else if (fld.getType() == Boolean.TYPE) fieldType = Boolean.class;
            else if (fld.getType() == Byte.TYPE) fieldType = Byte.class;
            else if (fld.getType() == Short.TYPE) fieldType = Short.class;
            else if (fld.getType() == Integer.TYPE) fieldType = Integer.class;
            else if (fld.getType() == Long.TYPE) fieldType = Long.class;
            else if (fld.getType() == Float.TYPE) fieldType = Float.class;
            else if (fld.getType() == Double.TYPE) fieldType = Double.class;
        }

        if (fld.getType() != fieldType) {
            throw new Error(
                "LMP field type is different! (" + from + " ." + field + ": expecting " + fieldType + ", but got "
                    + fld.getType() + ")"
            );
        }
    }
}
