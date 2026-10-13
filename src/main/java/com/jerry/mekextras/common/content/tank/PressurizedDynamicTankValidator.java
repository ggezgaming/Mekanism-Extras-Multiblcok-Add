package com.jerry.mekextras.common.content.tank;

import com.jerry.mekextras.common.config.ExtraConfig;
import com.jerry.mekextras.common.registries.ExtraBlockTypes;

import mekanism.common.content.blocktype.BlockType;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.multiblock.CuboidStructureValidator;
import mekanism.common.lib.multiblock.FormationProtocol;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PressurizedDynamicTankValidator extends CuboidStructureValidator<PressurizedDynamicTankMultiblockData> {

    public PressurizedDynamicTankValidator() {
        super(new VoxelCuboid(3, 3, 3), new VoxelCuboid(
                ExtraConfig.extraGeneralConfig.pressurizedDynamicTankMaxSize.get(),
                ExtraConfig.extraGeneralConfig.pressurizedDynamicTankMaxSize.get(),
                ExtraConfig.extraGeneralConfig.pressurizedDynamicTankMaxSize.get()));
    }

    @Override
    protected FormationProtocol.CasingType getCasingType(BlockState state) {
        Block block = state.getBlock();
        if (BlockType.is(block, ExtraBlockTypes.PRESSURIZED_DYNAMIC_TANK)) {
            return FormationProtocol.CasingType.FRAME;
        } else if (BlockType.is(block, ExtraBlockTypes.PRESSURIZED_DYNAMIC_VALVE)) {
            return FormationProtocol.CasingType.VALVE;
        }
        return FormationProtocol.CasingType.INVALID;
    }
}
