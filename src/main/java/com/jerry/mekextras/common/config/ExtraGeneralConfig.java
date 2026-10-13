package com.jerry.mekextras.common.config;

import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.MekanismConfigTranslations;
import mekanism.common.config.value.CachedIntValue;
import mekanism.common.config.value.CachedLongValue;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.fluids.FluidType;

public class ExtraGeneralConfig extends BaseMekanismConfig {

    private final ModConfigSpec configSpec;
    // Pump
    public final CachedIntValue pumpHeavyWaterAmount;
    // Pressurized Dynamic Tank
    public final CachedIntValue pressurizedDynamicTankMaxSize;
    public final CachedLongValue pressurizedDynamicTankFluidPerVolume;
    public final CachedLongValue pressurizedDynamicTankChemicalPerVolume;

    ExtraGeneralConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        MekanismConfigTranslations.GENERAL_PUMP.applyToBuilder(builder).push("pump");
        pumpHeavyWaterAmount = CachedIntValue.wrap(this, ExtraConfigTranslations.GENERAL_ADVANCED_PUMP_HEAVY_WATER.applyToBuilder(builder)
                .defineInRange("heavyWaterAmount", FluidType.BUCKET_VOLUME / 10, 1, FluidType.BUCKET_VOLUME));
        builder.pop();
        builder.push("pressurized_dynamic_tank");

        pressurizedDynamicTankMaxSize = CachedIntValue.wrap(
                this,
                ExtraConfigTranslations.PRESSURIZED_DYNAMIC_TANK_MAX_SIZE.applyToBuilder(builder)
                        .worldRestart()
                        .defineInRange("maxSize", 18, 3, 64));

        pressurizedDynamicTankFluidPerVolume = CachedLongValue.wrap(
                this,
                ExtraConfigTranslations.PRESSURIZED_DYNAMIC_TANK_FLUID_PER_VOLUME.applyToBuilder(builder)
                        .defineInRange("fluidPerVolume", 2_867_200_000L, 1L, Long.MAX_VALUE));

        pressurizedDynamicTankChemicalPerVolume = CachedLongValue.wrap(
                this,
                ExtraConfigTranslations.PRESSURIZED_DYNAMIC_TANK_CHEMICAL_PER_VOLUME.applyToBuilder(builder)
                        .defineInRange("chemicalPerVolume", 67_108_864_000_000L, 1L, Long.MAX_VALUE));

        builder.pop();

        configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "general";
    }

    @Override
    public String getTranslation() {
        return "General Config";
    }

    @Override
    public ModConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public ModConfig.Type getConfigType() {
        return ModConfig.Type.SERVER;
    }
}
