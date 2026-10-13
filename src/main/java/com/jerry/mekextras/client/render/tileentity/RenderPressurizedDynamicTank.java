package com.jerry.mekextras.client.render.tileentity;

import com.jerry.mekextras.common.content.tank.PressurizedDynamicTankMultiblockData;
import com.jerry.mekextras.common.tile.multiblock.TileEntityPressurizedDynamicTank;

import mekanism.api.annotations.NothingNullByDefault;
import mekanism.client.render.data.RenderData;
import mekanism.client.render.tileentity.MultiblockTileEntityRenderer;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.capabilities.merged.MergedTank.CurrentType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class RenderPressurizedDynamicTank extends
                                          MultiblockTileEntityRenderer<PressurizedDynamicTankMultiblockData, TileEntityPressurizedDynamicTank> {

    public RenderPressurizedDynamicTank(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(
                          TileEntityPressurizedDynamicTank tile,
                          PressurizedDynamicTankMultiblockData multiblock,
                          float partialTick,
                          PoseStack matrix,
                          MultiBufferSource renderer,
                          int light,
                          int overlayLight,
                          ProfilerFiller profiler) {
        RenderData data = getRenderData(multiblock);
        if (data != null) {
            VertexConsumer buffer = renderer.getBuffer(Sheets.translucentCullBlockSheet());
            renderObject(
                    data,
                    multiblock.valves,
                    tile.getBlockPos(),
                    matrix,
                    buffer,
                    overlayLight,
                    multiblock.prevScale);
        }
    }

    @Nullable
    private RenderData getRenderData(PressurizedDynamicTankMultiblockData multiblock) {
        CurrentType currentType = multiblock.mergedTank.getCurrentType();
        if (currentType == CurrentType.EMPTY) {
            return null;
        }

        return (switch (currentType) {
            case FLUID -> RenderData.Builder.create(multiblock.getFluidTank().getFluid());
            case CHEMICAL -> RenderData.Builder.create(multiblock.getChemicalTank().getStack());
            default -> throw new IllegalStateException("Unknown current type.");
        }).of(multiblock).build();
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.DYNAMIC_TANK;
    }

    @Override
    protected boolean shouldRender(
                                   TileEntityPressurizedDynamicTank tile,
                                   PressurizedDynamicTankMultiblockData multiblock,
                                   Vec3 camera) {
        return super.shouldRender(tile, multiblock, camera) && !multiblock.isEmpty();
    }
}
