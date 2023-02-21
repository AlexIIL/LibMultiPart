/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.multipart.compat.waila;

import org.jetbrains.annotations.Nullable;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IModInfo;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaConfig;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaConstants;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.MultipartUtil;
import alexiil.mc.lib.multipart.impl.MultipartBlock;

@SuppressWarnings("unused")
public class LibMultiPartPlugin implements IWailaPlugin, IBlockComponentProvider {
    @Override
    public void register(IRegistrar registrar) {
        registrar.addComponent(this, TooltipPosition.HEAD, MultipartBlock.class);
        registrar.addComponent(this, TooltipPosition.TAIL, MultipartBlock.class);
    }

    @Override
    public void appendHead(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        AbstractPart part = getSelectedPart(accessor);
        if (part == null) {
            return;
        }

        HitResult hitResult = accessor.getHitResult();
        String name =
            part.getName(hitResult instanceof BlockHitResult blockHitResult ? blockHitResult : null).getString();

        IWailaConfig.Formatter formatter = IWailaConfig.get().getFormatter();
        tooltip.setLine(WailaConstants.OBJECT_NAME_TAG, formatter.blockName(name));
        if (config.getBoolean(WailaConstants.CONFIG_SHOW_REGISTRY)) {
            tooltip.setLine(WailaConstants.REGISTRY_NAME_TAG, formatter.registryName(part.definition.identifier));
        }
    }

    @Override
    public void appendTail(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        if (config.getBoolean(WailaConstants.CONFIG_SHOW_MOD_NAME)) {
            AbstractPart selectedPart = getSelectedPart(accessor);

            if (selectedPart != null) {
                tooltip.setLine(
                    WailaConstants.MOD_NAME_TAG,
                    IWailaConfig.get().getFormatter().modName(IModInfo.get(selectedPart.definition.identifier).getName())
                );
            }
        }
    }

    private static @Nullable AbstractPart getSelectedPart(IBlockAccessor accessor) {
        BlockPos pos = accessor.getPosition();
        MultipartContainer container = MultipartUtil.get(accessor.getWorld(), pos);

        if (container == null) {
            return null;
        }

        Vec3d vec = accessor.getHitResult().getPos().subtract(Vec3d.of(pos));
        return container.getFirstPart(part -> doesContain(part, vec));
    }

    private static boolean doesContain(AbstractPart part, Vec3d vec) {
        VoxelShape shape = part.getOutlineShape();
        for (Box box : shape.getBoundingBoxes()) {
            if (box.expand(0.01).contains(vec)) {
                return true;
            }
        }
        return false;
    }
}
