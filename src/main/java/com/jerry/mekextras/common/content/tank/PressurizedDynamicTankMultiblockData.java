package com.jerry.mekextras.common.content.tank;

import com.jerry.mekextras.common.config.ExtraConfig;

import mekanism.api.IContentsListener;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.chemical.BigIntegerChemicalTank;
import mekanism.common.capabilities.fluid.BigIntegerFluidTank;
import mekanism.common.capabilities.fluid.FluidTankWrapper;
import mekanism.common.capabilities.merged.ChemicalTankWrapper;
import mekanism.common.capabilities.merged.MergedTank;
import mekanism.common.capabilities.merged.MergedTank.CurrentType;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.SyntheticComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.inventory.slot.HybridInventorySlot;
import mekanism.common.lib.multiblock.IValveHandler;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.tile.interfaces.IFluidContainerManager.ContainerEditMode;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.datafixers.util.Either;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PressurizedDynamicTankMultiblockData extends MultiblockData implements IValveHandler {

    private CurrentType lastSyncedType = null;
    private BigInteger lastSyncedStoredAmount = BigInteger.valueOf(-1);
    private int lastSyncedVolume = -1;

    @ContainerSync
    public final MergedTank mergedTank;

    @ContainerSync
    @SyntheticComputerMethod(getter = "getContainerEditMode")
    public ContainerEditMode editMode = ContainerEditMode.BOTH;

    @WrappingComputerMethod(
                            wrapper = ComputerIInventorySlotWrapper.class,
                            methodNames = "getInputItem",
                            docPlaceholder = "input slot")
    HybridInventorySlot inputSlot;

    @WrappingComputerMethod(
                            wrapper = ComputerIInventorySlotWrapper.class,
                            methodNames = "getOutputItem",
                            docPlaceholder = "output slot")
    HybridInventorySlot outputSlot;

    protected int tankCapacity;
    protected long chemicalTankCapacity;

    protected BigInteger exactTankCapacity = BigInteger.ZERO;
    protected BigInteger exactChemicalTankCapacity = BigInteger.ZERO;

    public float prevScale;

    public PressurizedDynamicTankMultiblockData(
                                                TileEntityMultiblock<PressurizedDynamicTankMultiblockData> tile) {
        super(tile);

        IContentsListener saveAndComparator = createSaveAndComparator();

        mergedTank = MergedTank.create(
                BigIntegerFluidTank.create(
                        this,
                        this::getExactTankCapacityValue,
                        ConstantPredicates.alwaysTrue(),
                        saveAndComparator),
                BigIntegerChemicalTank.create(
                        this,
                        this::getExactChemicalTankCapacityValue,
                        ConstantPredicates.alwaysTrue(),
                        null,
                        saveAndComparator));

        fluidTanks.add(mergedTank.getFluidTank());
        chemicalTanks.add(mergedTank.getChemicalTank());
        inventorySlots.addAll(createBaseInventorySlots());
    }

    private List<IInventorySlot> createBaseInventorySlots() {
        List<IInventorySlot> slots = new ArrayList<>();

        slots.add(inputSlot = HybridInventorySlot.inputOrDrain(mergedTank, this, 146, 21));
        slots.add(outputSlot = HybridInventorySlot.outputOrFill(mergedTank, this, 146, 51));

        inputSlot.setSlotType(ContainerSlotType.INPUT);
        outputSlot.setSlotType(ContainerSlotType.OUTPUT);

        return slots;
    }

    @Override
    public boolean tick(Level world) {
        boolean needsPacket = super.tick(world);

        CurrentType type = mergedTank.getCurrentType();

        if (type == CurrentType.EMPTY) {
            inputSlot.handleTank(outputSlot, editMode);
            inputSlot.handleChemicalTank(outputSlot, editMode);
        } else if (type == CurrentType.FLUID) {
            inputSlot.handleTank(outputSlot, editMode);
        } else {
            inputSlot.handleChemicalTank(outputSlot, editMode);
        }

        CurrentType currentType = mergedTank.getCurrentType();
        BigInteger storedAmount = getExactStoredAmountValue();
        int volume = getVolume();

        if (currentType != lastSyncedType || !storedAmount.equals(lastSyncedStoredAmount) || volume != lastSyncedVolume) {

            lastSyncedType = currentType;
            lastSyncedStoredAmount = storedAmount;
            lastSyncedVolume = volume;
            needsPacket = true;
        }

        float scale = getScale();
        if (MekanismUtils.scaleChanged(scale, prevScale)) {
            prevScale = scale;
            needsPacket = true;
        }

        return needsPacket;
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.readUpdateTag(tag, provider);
        if (tag.contains("pressurizedFluidCapacity")) {
            exactTankCapacity = new BigInteger(tag.getString("pressurizedFluidCapacity"));
            tankCapacity = exactTankCapacity.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        }

        if (tag.contains("pressurizedChemicalCapacity")) {
            exactChemicalTankCapacity = new BigInteger(tag.getString("pressurizedChemicalCapacity"));
            chemicalTankCapacity = exactChemicalTankCapacity.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
        }

        NBTUtils.setFloatIfPresent(tag, SerializationConstants.SCALE, scale -> prevScale = scale);
        mergedTank.readFromUpdateTag(provider, tag);
        readValves(tag);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeUpdateTag(tag, provider);

        tag.putFloat(SerializationConstants.SCALE, prevScale);
        mergedTank.addToUpdateTag(provider, tag);
        writeValves(tag);
        tag.putString("pressurizedFluidCapacity", exactTankCapacity.toString());
        tag.putString("pressurizedChemicalCapacity", exactChemicalTankCapacity.toString());
    }

    private float getScale() {
        return switch (mergedTank.getCurrentType()) {
            case FLUID -> getExactScale(prevScale, getExactStoredAmountValue(), exactTankCapacity);
            case CHEMICAL -> getExactScale(prevScale, getExactStoredAmountValue(), exactChemicalTankCapacity);
            default -> MekanismUtils.getScale(prevScale, 0, chemicalTankCapacity, true);
        };
    }

    private float getExactScale(float previousScale, BigInteger stored, BigInteger capacity) {
        float targetScale = capacity.signum() == 0 ? 0 : (float) (stored.doubleValue() / capacity.doubleValue());

        return MekanismUtils.getScale(previousScale, targetScale, stored.signum() == 0, stored.equals(capacity));
    }

    @ComputerMethod
    public int getTankCapacity() {
        return tankCapacity;
    }

    @ComputerMethod
    public long getChemicalTankCapacity() {
        return chemicalTankCapacity;
    }

    @ComputerMethod
    public String getExactTankCapacity() {
        return exactTankCapacity.toString();
    }

    public BigInteger getExactTankCapacityValue() {
        return exactTankCapacity;
    }

    @ComputerMethod
    public String getExactChemicalTankCapacity() {
        return exactChemicalTankCapacity.toString();
    }

    public BigInteger getExactChemicalTankCapacityValue() {
        return exactChemicalTankCapacity;
    }

    @ComputerMethod
    public String getExactStoredAmount() {
        return switch (mergedTank.getCurrentType()) {
            case FLUID -> ((BigIntegerFluidTank) ((FluidTankWrapper) mergedTank.getFluidTank()).getInternalTank())
                    .getExactStoredAmount().toString();
            case CHEMICAL -> ((BigIntegerChemicalTank) ((ChemicalTankWrapper) mergedTank.getChemicalTank()).getInternalTank())
                    .getExactStoredAmount().toString();
            default -> "0";
        };
    }

    public BigInteger getExactStoredAmountValue() {
        return switch (mergedTank.getCurrentType()) {
            case FLUID -> ((BigIntegerFluidTank) ((FluidTankWrapper) mergedTank.getFluidTank()).getInternalTank())
                    .getExactStoredAmount();
            case CHEMICAL -> ((BigIntegerChemicalTank) ((ChemicalTankWrapper) mergedTank.getChemicalTank()).getInternalTank())
                    .getExactStoredAmount();
            default -> BigInteger.ZERO;
        };
    }

    @Override
    public void setVolume(int volume) {
        if (getVolume() != volume) {
            super.setVolume(volume);
        }
        recalculateCapacities();
    }

    private void recalculateCapacities() {
        exactTankCapacity = BigInteger.valueOf(getVolume()).multiply(
                BigInteger.valueOf(ExtraConfig.extraGeneralConfig.pressurizedDynamicTankFluidPerVolume.get()));

        exactChemicalTankCapacity = BigInteger.valueOf(getVolume()).multiply(
                BigInteger.valueOf(ExtraConfig.extraGeneralConfig.pressurizedDynamicTankChemicalPerVolume.get()));

        tankCapacity = exactTankCapacity.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        chemicalTankCapacity = exactChemicalTankCapacity.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    @Override
    protected int getMultiblockRedstoneLevel() {
        BigInteger capacity = mergedTank.getCurrentType() == CurrentType.FLUID ? exactTankCapacity : exactChemicalTankCapacity;

        BigInteger stored = getExactStoredAmountValue();

        if (capacity.signum() == 0) {
            return 0;
        }

        return MekanismUtils.redstoneLevelFromContents(
                Math.round(stored.doubleValue() / capacity.doubleValue() * 1_000_000),
                1_000_000);
    }

    public IExtendedFluidTank getFluidTank() {
        return mergedTank.getFluidTank();
    }

    public IChemicalTank getChemicalTank() {
        return mergedTank.getChemicalTank();
    }

    public boolean isEmpty() {
        return mergedTank.getCurrentType() == CurrentType.EMPTY;
    }

    @ComputerMethod
    public void setContainerEditMode(ContainerEditMode mode) {
        if (editMode != mode) {
            editMode = mode;
            markDirty();
        }
    }

    @ComputerMethod
    void incrementContainerEditMode() {
        setContainerEditMode(editMode.getNext());
    }

    @ComputerMethod
    void decrementContainerEditMode() {
        setContainerEditMode(editMode.getPrevious());
    }

    @ComputerMethod
    Either<ChemicalStack, FluidStack> getStored() {
        return switch (mergedTank.getCurrentType()) {
            case FLUID -> Either.right(getFluidTank().getFluid());
            case CHEMICAL -> Either.left(getChemicalTank().getStack());
            default -> Either.right(FluidStack.EMPTY);
        };
    }

    @ComputerMethod
    double getFilledPercentage() {
        BigInteger capacity = mergedTank.getCurrentType() == CurrentType.FLUID ? exactTankCapacity : exactChemicalTankCapacity;

        return capacity.signum() == 0 ? 0 : getExactStoredAmountValue().doubleValue() / capacity.doubleValue();
    }

    @Override
    public Collection<IValveHandler.ValveData> getValveData() {
        return valves;
    }
}
