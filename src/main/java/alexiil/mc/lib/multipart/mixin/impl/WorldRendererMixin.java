/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;
import alexiil.mc.lib.multipart.mixin.api.IWorldRendererMixin;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements IWorldRendererMixin {

    private static final String WORLD_RENDERER = "Lnet/minecraft/client/render/WorldRenderer;";

    private static final String MATRIX_STACK = "Lnet/minecraft/client/util/math/MatrixStack;";
    private static final String VERTEX_CONSUMER = "Lnet/minecraft/client/render/VertexConsumer;";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";
    private static final String VOXEL_SHAPE = "Lnet/minecraft/util/shape/VoxelShape;";
    private static final String BLOCK_POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String BLOCK_STATE = "Lnet/minecraft/block/BlockState;";

    private static final String CAMERA = "Lnet/minecraft/client/render/Camera;";
    private static final String GAME_RENDERER = "Lnet/minecraft/client/render/GameRenderer;";
    private static final String LIGHTMAP_TEXTURE_MANAGER = "Lnet/minecraft/client/render/LightmapTextureManager;";
    private static final String MATRIX4F = "Lnet/minecraft/util/math/Matrix4f;";

    private static final String _M_DRAW_BLOCK_OUTLINE
        = "drawBlockOutline(" + MATRIX_STACK + VERTEX_CONSUMER + ENTITY + "DDD" + BLOCK_POS + BLOCK_STATE + ")V";

    private static final String _M_DRAW_SHAPE_OUTLINE
        = "drawShapeOutline(" + MATRIX_STACK + VERTEX_CONSUMER + VOXEL_SHAPE + "DDDFFFF)V";

    @Shadow
    private ClientWorld world;

    @Unique
    @Deprecated
    private boolean drawingBlockOutline;

    @Override
    @Deprecated
    public boolean libmultipart_isDrawingBlockOutline() {
        return drawingBlockOutline;
    }

    @ModifyArg(at = @At(value = "INVOKE", target = WORLD_RENDERER + _M_DRAW_SHAPE_OUTLINE),
        method = WORLD_RENDERER + _M_DRAW_BLOCK_OUTLINE)
    private VoxelShape modifyShape(VoxelShape shape) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.BLOCK) {
            BlockHitResult bhit = (BlockHitResult) mc.crosshairTarget;
            BlockPos bpos = bhit.getBlockPos();
            Vec3d bvec = bhit.getPos();
            BlockState state = world.getBlockState(bpos);
            if (state.getBlock() instanceof IBlockMultipart<?>) {
                IBlockMultipart<?> multipart = ((IBlockMultipart<?>) state.getBlock());
                VoxelShape partShape = multipart.getPartOutline(state, world, bpos, bvec);
                if (!partShape.isEmpty()) {
                    return partShape;
                }
            }
        }

        return shape;
    }

    @Inject(at = { @At("HEAD") }, method = _M_DRAW_BLOCK_OUTLINE)
    private void beginBlockOutline(CallbackInfo ci) {
        if (drawingBlockOutline) {
            throw new IllegalStateException(
                "[LibMultiPart] We've gotten out of sync! (Expected to *NOT* be in the middle of drawing block outlines when we are!)"
            );
        }
        drawingBlockOutline = true;
    }

    @Inject(at = { @At(value = "INVOKE", shift = Shift.AFTER, target = WORLD_RENDERER + _M_DRAW_BLOCK_OUTLINE) },
        method = WORLD_RENDERER + "render(" + MATRIX_STACK + "FJZ" + CAMERA + GAME_RENDERER + LIGHTMAP_TEXTURE_MANAGER
            + MATRIX4F + ")V")
    private void endBlockOutline(CallbackInfo ci) {
        if (!drawingBlockOutline) {
            throw new IllegalStateException(
                "[LibMultiPart] We've gotten out of sync! (Expected to be in the middle of drawing block outlines when we're not!)"
            );
        }
        drawingBlockOutline = false;
    }
}
