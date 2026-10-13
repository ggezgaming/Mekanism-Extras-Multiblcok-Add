package com.jerry.mekextras.common.tile.multiblock;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.common.content.tank.PressurizedDynamicTankMultiblockData;
import com.jerry.mekextras.common.registries.ExtraBlocks;

import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class TileEntityPressurizedDynamicValve
                                               extends TileEntityMultiblock<PressurizedDynamicTankMultiblockData> {

    public TileEntityPressurizedDynamicValve(BlockPos pos, BlockState state) {
        this(ExtraBlocks.PRESSURIZED_DYNAMIC_VALVE, pos, state);
    }

    public TileEntityPressurizedDynamicValve(
                                             Holder<Block> blockProvider,
                                             BlockPos pos,
                                             BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    public boolean canBeMaster() {
        // The GUI is registered for the tank casing tile, not the valve.
        return false;
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
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        return side -> getMultiblock().getFluidTanks(side);
    }

    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        return side -> getMultiblock().getChemicalTanks(side);
    }

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        if (type == ContainerType.FLUID || type == ContainerType.CHEMICAL) {
            return false;
        }
        return super.persists(type);
    }

    @Override
    public FluidStack insertFluid(int tank, FluidStack stack, Direction side, Action action) {
        return handleValveTransfer(
                stack,
                action,
                super.insertFluid(tank, stack, side, action));
    }

    @Override
    public FluidStack insertFluid(FluidStack stack, Direction side, Action action) {
        return handleValveTransfer(
                stack,
                action,
                super.insertFluid(stack, side, action));
    }

    private FluidStack handleValveTransfer(
                                           FluidStack stack,
                                           Action action,
                                           FluidStack remainder) {
        if (action.execute() && remainder.getAmount() < stack.getAmount()) {
            getMultiblock().triggerValveTransfer(this);
        }
        return remainder;
    }

    @Override
    public int getRedstoneLevel() {
        return getMultiblock().getCurrentRedstoneLevel();
    }

    @Override
    public ItemInteractionResult onActivate(Player player, InteractionHand hand, ItemStack stack) {
        PressurizedDynamicTankMultiblockData multiblock = getMultiblock();
        if (!player.isShiftKeyDown() && multiblock.isFormed()) {
            // The valve has its own tile entity, but the GUI is registered for the tank casing.
            // Open it through the master casing so the GUI receives the full multiblock data.
            TileEntityPressurizedDynamicTank guiTile = null;
            if (level != null && !level.isClientSide) {
                for (BlockPos pos : multiblock.locations) {
                    BlockEntity candidate = level.getBlockEntity(pos);
                    if (candidate instanceof TileEntityPressurizedDynamicTank tank && tank.isMaster()) {
                        guiTile = tank;
                        break;
                    }
                }
            }
            if (guiTile == null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
