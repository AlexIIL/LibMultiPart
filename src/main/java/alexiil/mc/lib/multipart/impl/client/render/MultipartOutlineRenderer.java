/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.impl.client.render;

import alexiil.mc.lib.multipart.mixin.api.IBlockMultipart;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

import java.util.Objects;

/**
 * Renders custom block outlines for only the selected part in a multipart block.
 */
public final class MultipartOutlineRenderer implements WorldRenderEvents.BlockOutline {
    public static final MultipartOutlineRenderer INSTANCE = new MultipartOutlineRenderer();

    private MultipartOutlineRenderer() {
    }

    @Override
    public boolean onBlockOutline(WorldRenderContext worldRenderContext,
                                  WorldRenderContext.BlockOutlineContext blockOutlineContext) {

        // In theory, this should never be null by this point, but who knows what custom renderers could be doing.
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = blockOutlineContext.blockState();
            Block block = state.getBlock();

            if (block instanceof IBlockMultipart<?> multipart) {
                BlockPos blockPos = blockOutlineContext.blockPos();
                VoxelShape partShape =
                        multipart.getPartOutline(state, worldRenderContext.world(), blockPos, hit.getPos());

                if (!partShape.isEmpty()) {
                    VertexConsumer linesConsumer =
                            Objects.requireNonNull(worldRenderContext.consumers(), "consumers is null")
                                    .getBuffer(RenderLayer.getLines());
                    drawShapeOutline(worldRenderContext.matrixStack(), linesConsumer, partShape,
                            blockPos.getX() - blockOutlineContext.cameraX(),
                            blockPos.getY() - blockOutlineContext.cameraY(),
                            blockPos.getZ() - blockOutlineContext.cameraZ());

                    return false;
                }
            }
        }
        return true;
    }

    private static void drawShapeOutline(MatrixStack matrices, VertexConsumer vertexConsumer, VoxelShape voxelShape,
                                         double offsetX, double offsetY, double offsetZ) {
        MatrixStack.Entry entry = matrices.peek();
        voxelShape.forEachEdge((startX, startY, startZ, endX, endY, endZ) -> {
            float nx = (float) (endX - startX);
            float ny = (float) (endY - startY);
            float nz = (float) (endZ - startZ);

            float t = MathHelper.fastInverseSqrt(nx * nx + ny * ny + nz * nz);

            nx *= t;
            ny *= t;
            nz *= t;

            vertexConsumer.vertex(entry.getPositionMatrix(), (float) (startX + offsetX), (float) (startY + offsetY),
                            (float) (startZ + offsetZ))
                    .color(0f, 0f, 0f, 0.4f)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();
            vertexConsumer.vertex(entry.getPositionMatrix(), (float) (endX + offsetX), (float) (endY + offsetY),
                            (float) (endZ + offsetZ))
                    .color(0f, 0f, 0f, 0.4f)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();
        });
    }
}
