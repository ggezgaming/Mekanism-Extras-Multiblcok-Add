package com.jerry.mekextras.common.tile.multiblock;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.common.content.tank.PressurizedDynamicTankMultiblockData;
import com.jerry.mekextras.common.registries.ExtraBlocks;

import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.interfaces.IFluidContainerManager;
import mekanism.common.tile.interfaces.IFluidContainerManager.ContainerEditMode;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.FluidUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityPressurizedDynamicTank
                                              extends TileEntityMultiblock<PressurizedDynamicTankMultiblockData>
                                              implements IFluidContainerManager {

    public TileEntityPressurizedDynamicTank(BlockPos pos, BlockState state) {
        this(ExtraBlocks.PRESSURIZED_DYNAMIC_TANK, pos, state);
    }

    public TileEntityPressurizedDynamicTank(
                                            Holder<net.minecraft.world.level.block.Block> blockProvider,
                                            BlockPos pos,
                                            BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    public PressurizedDynamicTankMultiblockData createMultiblock() {
        return new PressurizedDynamicTankMultiblockData(this);
    }

    @Override
    public MultiblockManager<PressurizedDynamicTankMultiblockData> getManager() {
        return MekanismExtras.pressurizedDynamicTankManager;
    }

    @Override
    public ContainerEditMode getContainerEditMode() {
        return getMultiblock().editMode;
    }

    @Override
    public void nextMode() {
        var multiblock = getMultiblock();
        multiblock.setContainerEditMode(multiblock.editMode.getNext());
    }

    @Override
    public void previousMode() {
        var multiblock = getMultiblock();
        multiblock.setContainerEditMode(multiblock.editMode.getPrevious());
    }

    @Override
    public ItemInteractionResult onActivate(
                                            Player player,
                                            InteractionHand hand,
                                            ItemStack stack) {
        PressurizedDynamicTankMultiblockData multiblock = getMultiblock();
        if (!player.isShiftKeyDown() && multiblock.isFormed()) {
            // Match Mekanism's regular Dynamic Tank fluid-container interaction.
            if (FluidUtils.handleTankInteraction(player, hand, stack, multiblock.getFluidTank())) {
                player.getInventory().setChanged();
                return ItemInteractionResult.SUCCESS;
            }

            // Open the GUI from the master casing so the client receives the full
            // multiblock update tag, even when the player clicks a non-master block.
            TileEntityPressurizedDynamicTank guiTile = this;
            if (level != null && !level.isClientSide) {
                for (BlockPos pos : multiblock.locations) {
                    BlockEntity candidate = level.getBlockEntity(pos);
                    if (candidate instanceof TileEntityPressurizedDynamicTank tank && tank.isMaster()) {
                        guiTile = tank;
                        break;
                    }
                }
            }
            InteractionResult result = guiTile.openGui(player);
            return switch (result) {
                case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
                case CONSUME -> ItemInteractionResult.CONSUME;
                case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
                case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                case FAIL -> ItemInteractionResult.FAIL;
            };
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
