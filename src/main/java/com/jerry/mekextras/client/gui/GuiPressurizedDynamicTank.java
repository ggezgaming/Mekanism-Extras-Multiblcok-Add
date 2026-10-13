package com.jerry.mekextras.client.gui;

import com.jerry.mekextras.common.content.tank.PressurizedDynamicTankMultiblockData;
import com.jerry.mekextras.common.tile.multiblock.TileEntityPressurizedDynamicTank;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiSideHolder;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiMergedTankGauge;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.tab.GuiContainerEditModeTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.text.TankAmountFormatter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GuiPressurizedDynamicTank extends
                                       GuiMekanismTile<TileEntityPressurizedDynamicTank, MekanismTileContainer<TileEntityPressurizedDynamicTank>> {

    public GuiPressurizedDynamicTank(
                                     MekanismTileContainer<TileEntityPressurizedDynamicTank> container,
                                     Inventory inv,
                                     Component title) {
        super(container, inv, title);
        inventoryLabelY += 2;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        addRenderableWidget(GuiSideHolder.armorHolder(this));
        addRenderableWidget(new GuiElementHolder(this, 141, 16, 26, 56));

        super.addGuiElements();

        addRenderableWidget(new GuiSlot(SlotType.INNER_HOLDER_SLOT, this, 145, 20));
        addRenderableWidget(new GuiSlot(SlotType.INNER_HOLDER_SLOT, this, 145, 50));

        addRenderableWidget(new GuiInnerScreen(this, 49, 21, 84, 46, () -> getTankInfo(true))
                .tooltip(() -> getTankInfo(false)).spacing(1));

        addRenderableWidget(new GuiDownArrow(this, 150, 39));
        addRenderableWidget(new GuiContainerEditModeTab<>(this, tile));

        addRenderableWidget(new GuiMergedTankGauge<>(
                () -> tile.getMultiblock().mergedTank,
                tile::getMultiblock,
                GaugeType.MEDIUM,
                this,
                7, 16, 34, 56));
    }

    private List<Component> getTankInfo(boolean compact) {
        List<Component> ret = new ArrayList<>();
        PressurizedDynamicTankMultiblockData multiblock = tile.getMultiblock();

        BigInteger fluidCapacity = multiblock.getExactTankCapacityValue();
        BigInteger chemicalCapacity = multiblock.getExactChemicalTankCapacityValue();
        BigInteger capacity;

        switch (multiblock.mergedTank.getCurrentType()) {
            case EMPTY -> {
                ret.add(MekanismLang.EMPTY.translate());
                capacity = fluidCapacity.min(chemicalCapacity);
            }
            case FLUID -> {
                ret.add(MekanismLang.GENERIC_PRE_COLON.translate(multiblock.mergedTank.getFluidTank().getFluid()));
                BigInteger stored = new BigInteger(multiblock.getExactStoredAmount());
                ret.add(Component.literal(compact ? TankAmountFormatter.format(stored) : TankAmountFormatter.formatExact(stored)));
                capacity = fluidCapacity;
            }
            case CHEMICAL -> {
                ret.add(MekanismLang.GENERIC_PRE_COLON.translate(multiblock.mergedTank.getChemicalTank().getStack()));
                BigInteger stored = new BigInteger(multiblock.getExactStoredAmount());
                ret.add(Component.literal(compact ? TankAmountFormatter.format(stored) : TankAmountFormatter.formatExact(stored)));
                capacity = chemicalCapacity;
            }
            default -> throw new IllegalStateException("Unknown Pressurized Dynamic Tank content type");
        }

        ret.add(MekanismLang.CAPACITY.translate(""));
        ret.add(Component.literal(
                compact ? TankAmountFormatter.format(capacity) : TankAmountFormatter.formatExact(capacity)));

        return ret;
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
